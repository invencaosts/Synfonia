import re
import time

from fastapi import Body, FastAPI, HTTPException
from pydantic import BaseModel
from ytmusicapi import YTMusic
from ytmusicapi.auth.oauth import OAuthCredentials

app = FastAPI(title="Synfonia YT Music Service")
yt = YTMusic()

# Client "TV/dispositivo de entrada limitada" público, mantido pela própria
# ytmusicapi. Não é segredo (é o mesmo client usado por qualquer instalação
# da lib) — dá pra trocar por um client próprio via env se algum dia precisar.
oauth_credentials = OAuthCredentials()

FILTER_MAP = {
    "title": "songs",
    "artist": "artists",
    "album": "albums",
}

THUMBNAIL_SIZE_RE = re.compile(r"=w\d+-h\d+(-l\d+)?(-rj)?$")


def best_thumbnail(thumbnails, size=544):
    if not thumbnails:
        return None
    url = thumbnails[-1].get("url")
    if not url:
        return None
    # Busca só devolve capas pequenas (60x60/120x120); o CDN do Google aceita
    # qualquer tamanho no parâmetro da própria URL, então pedimos uma maior.
    if THUMBNAIL_SIZE_RE.search(url):
        return THUMBNAIL_SIZE_RE.sub(f"=w{size}-h{size}-l90-rj", url)
    return url


def artist_names(artists):
    return ", ".join(a.get("name", "") for a in artists or [] if a.get("name"))


def normalize(item):
    result_type = item.get("resultType")

    if result_type == "artist":
        browse_id = item.get("browseId")
        return {
            "id": browse_id,
            "nome": item.get("artist"),
            "artista": item.get("artist"),
            "album": None,
            "capaUrl": best_thumbnail(item.get("thumbnails")),
            "previewUrl": None,
            "uri": f"https://music.youtube.com/channel/{browse_id}" if browse_id else None,
            "source": "YOUTUBE_MUSIC",
        }

    if result_type == "album":
        browse_id = item.get("browseId")
        return {
            "id": browse_id,
            "nome": item.get("title"),
            "artista": artist_names(item.get("artists")),
            "album": item.get("title"),
            "capaUrl": best_thumbnail(item.get("thumbnails")),
            "previewUrl": None,
            "uri": f"https://music.youtube.com/browse/{browse_id}" if browse_id else None,
            "source": "YOUTUBE_MUSIC",
        }

    video_id = item.get("videoId")
    return {
        "id": video_id,
        "nome": item.get("title"),
        "artista": artist_names(item.get("artists")),
        "album": (item.get("album") or {}).get("name"),
        "capaUrl": best_thumbnail(item.get("thumbnails")),
        "previewUrl": None,
        "uri": f"https://music.youtube.com/watch?v={video_id}" if video_id else None,
        "source": "YOUTUBE_MUSIC",
    }


@app.get("/search")
def search(q: str, tipo: str = "all", limit: int = 20):
    yt_filter = FILTER_MAP.get(tipo)

    if yt_filter is not None:
        results = yt.search(q, filter=yt_filter, limit=limit)
    else:
        # Sem filter=None: essa chamada aciona o parser de "top result" da
        # ytmusicapi, que quebra (KeyError: 'header') com o layout atual do
        # YouTube Music. Buscamos songs/albums/artists em separado e juntamos.
        results = []
        for f in ("songs", "albums", "artists"):
            try:
                results.extend(yt.search(q, filter=f, limit=limit))
            except Exception:
                continue

    normalized = [normalize(r) for r in results]
    return [r for r in normalized if r["id"]]


@app.get("/track/{video_id}")
def get_track(video_id: str):
    info = yt.get_song(video_id)
    details = info.get("videoDetails", {})
    return {
        "id": details.get("videoId", video_id),
        "nome": details.get("title"),
        "artista": details.get("author"),
        "album": None,
        "capaUrl": best_thumbnail(details.get("thumbnail", {}).get("thumbnails")),
        "previewUrl": None,
        "uri": f"https://music.youtube.com/watch?v={video_id}",
        "source": "YOUTUBE_MUSIC",
    }


def normalize_playlist(item):
    playlist_id = item.get("playlistId")
    return {
        "id": playlist_id,
        "nome": item.get("title"),
        "capaUrl": best_thumbnail(item.get("thumbnails")),
        "totalFaixas": item.get("count"),
        "uri": f"https://music.youtube.com/playlist?list={playlist_id}" if playlist_id else None,
        "source": "YOUTUBE_MUSIC",
    }


class DevicePollRequest(BaseModel):
    device_code: str


class RefreshRequest(BaseModel):
    refresh_token: str


class TokenRequest(BaseModel):
    token: dict


def build_token_dict(raw: dict, previous_refresh_token: str | None = None):
    if "error" in raw:
        return None
    token = dict(raw)
    token["expires_at"] = int(time.time()) + int(token.get("expires_in", 0))
    # A resposta de refresh do Google normalmente não devolve refresh_token de novo (reaproveita o antigo).
    token.setdefault("refresh_token", previous_refresh_token)
    return token


def authenticated_client(token: dict):
    if not token or not token.get("access_token"):
        raise HTTPException(status_code=401, detail="Token do YouTube Music ausente ou inválido.")
    return YTMusic(auth=token, oauth_credentials=oauth_credentials)


@app.post("/auth/device/start")
def start_device_auth():
    """Primeiro passo do fluxo OAuth estilo TV: gera o código que o usuário digita em google.com/device."""
    try:
        code = oauth_credentials.get_code()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao iniciar autenticação com o Google: {e}")
    return code


@app.post("/auth/device/poll")
def poll_device_auth(payload: DevicePollRequest):
    """Segundo passo: verifica se o usuário já autorizou o device_code."""
    try:
        raw = oauth_credentials.token_from_code(payload.device_code)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao consultar autorização: {e}")

    if "error" in raw:
        # authorization_pending / slow_down são esperados enquanto o usuário não autorizou ainda.
        return {"status": "pending", "reason": raw.get("error")}

    token = build_token_dict(raw)
    return {"status": "authorized", "token": token}


@app.post("/auth/refresh")
def refresh_auth(payload: RefreshRequest):
    try:
        raw = oauth_credentials.refresh_token(payload.refresh_token)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao renovar token: {e}")

    token = build_token_dict(raw, previous_refresh_token=payload.refresh_token)
    if token is None:
        raise HTTPException(status_code=401, detail=f"Refresh token inválido/expirado: {raw.get('error')}")
    return {"token": token}


@app.post("/me/playlists")
def get_my_playlists(payload: TokenRequest = Body(...)):
    client = authenticated_client(payload.token)
    try:
        playlists = client.get_library_playlists(limit=50)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao buscar playlists: {e}")
    return [normalize_playlist(p) for p in playlists]


@app.post("/me/playlists/{playlist_id}/tracks")
def get_playlist_tracks(playlist_id: str, payload: TokenRequest = Body(...)):
    client = authenticated_client(payload.token)
    try:
        data = client.get_playlist(playlist_id, limit=None)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao buscar faixas da playlist: {e}")
    tracks = data.get("tracks", [])
    return [normalize(t) for t in tracks if t.get("videoId")]


@app.post("/me/account")
def get_account(payload: TokenRequest = Body(...)):
    client = authenticated_client(payload.token)
    try:
        return client.get_account_info()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao buscar dados da conta: {e}")


@app.post("/me/liked-songs")
def get_my_liked_songs(payload: TokenRequest = Body(...)):
    client = authenticated_client(payload.token)
    try:
        data = client.get_liked_songs(limit=100)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Falha ao buscar músicas curtidas: {e}")
    tracks = data.get("tracks", [])
    return [normalize(t) for t in tracks if t.get("videoId")]


@app.get("/health")
def health():
    return {"status": "ok"}
