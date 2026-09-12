import re

from fastapi import FastAPI
from ytmusicapi import YTMusic

app = FastAPI(title="Synfonia YT Music Service")
yt = YTMusic()

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


@app.get("/health")
def health():
    return {"status": "ok"}
