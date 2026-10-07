#!/usr/bin/env python3
"""
Migração única dos dados do MongoDB para o PostgreSQL (só biblioteca padrão do Python 3.9+).

Passo a passo (detalhado em migrations/LEIA-ME.md):

  1. Exportar as coleções do Mongo (uma por arquivo, JSON relaxado, um documento por linha):
       for c in musicas playlists user_songs album_ratings historico_reproducao; do
         mongoexport --uri "$MONGO_URI" --collection "$c" --jsonFormat=relaxed --out "export/$c.json"
       done

  2. Gerar o SQL:
       python3 migrations/mongo_para_postgres.py export/ > dados_mongo.sql

  3. Aplicar (as tabelas já precisam existir: rode antes 2026-10-07_mongo_para_postgres.sql):
       psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f dados_mongo.sql

O SQL gerado:
  - roda numa transação só (tudo ou nada);
  - é idempotente (ON CONFLICT DO NOTHING): pode ser aplicado de novo sem duplicar;
  - descarta registros de usuários que não existem mais no Postgres (os "órfãos" do Mongo);
  - mantém os ids originais (ObjectId vira texto), então links de playlists continuam válidos;
  - no histórico, mantém só a reprodução mais recente de cada (usuário, faixa);
  - converte datas (gravadas em UTC pelo Mongo) para o fuso da aplicação (America/Sao_Paulo).
"""

import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

FUSO_APP = ZoneInfo("America/Sao_Paulo")


# ---------- leitura do mongoexport ----------

def ler_colecao(pasta: Path, nome: str):
    arquivo = pasta / f"{nome}.json"
    if not arquivo.exists():
        print(f"-- aviso: {arquivo} não encontrado, coleção ignorada", file=sys.stderr)
        return []
    texto = arquivo.read_text(encoding="utf-8").strip()
    if not texto:
        return []
    if texto.startswith("["):  # --jsonArray
        return json.loads(texto)
    return [json.loads(linha) for linha in texto.splitlines() if linha.strip()]


def oid(valor):
    """_id pode vir como {"$oid": "..."} ou string pura (ex.: id da faixa em musicas)."""
    if isinstance(valor, dict) and "$oid" in valor:
        return valor["$oid"]
    return None if valor is None else str(valor)


def numero(valor):
    if isinstance(valor, dict):
        for chave in ("$numberLong", "$numberInt", "$numberDouble", "$numberDecimal"):
            if chave in valor:
                return valor[chave]
    return valor


def data(valor):
    """{"$date": "2026-01-01T12:00:00Z"} ou {"$date": {"$numberLong": "..."}} -> timestamp local da aplicação."""
    if valor is None:
        return None
    bruto = valor.get("$date") if isinstance(valor, dict) else valor
    if isinstance(bruto, dict):
        bruto = int(numero(bruto))
    if isinstance(bruto, (int, float)):
        instante = datetime.fromtimestamp(bruto / 1000, tz=timezone.utc)
    else:
        instante = datetime.fromisoformat(str(bruto).replace("Z", "+00:00"))
        if instante.tzinfo is None:
            instante = instante.replace(tzinfo=timezone.utc)
    return instante.astimezone(FUSO_APP).replace(tzinfo=None).isoformat(sep=" ")


# ---------- geração de SQL ----------

def sql(valor):
    if valor is None:
        return "NULL"
    if isinstance(valor, bool):
        return "TRUE" if valor else "FALSE"
    if isinstance(valor, (int, float)):
        return repr(valor)
    return "'" + str(valor).replace("'", "''") + "'"


def inserir(tabela, colunas, valores, conflito="", exige_usuario=None, exige_playlist=None):
    """INSERT ... SELECT ... WHERE EXISTS (dono) ON CONFLICT [alvo] DO NOTHING (sem alvo = qualquer chave única)."""
    lista = ", ".join(colunas)
    selecao = ", ".join(sql(v) for v in valores)
    filtros = []
    if exige_usuario is not None:
        filtros.append(f"EXISTS (SELECT 1 FROM usuarios WHERE id = {sql(exige_usuario)})")
    if exige_playlist is not None:
        filtros.append(f"EXISTS (SELECT 1 FROM playlists WHERE id = {sql(exige_playlist)})")
    onde = f" WHERE {' AND '.join(filtros)}" if filtros else ""
    alvo = f" {conflito}" if conflito else ""
    return f"INSERT INTO {tabela} ({lista}) SELECT {selecao}{onde} ON CONFLICT{alvo} DO NOTHING;"


def gerar(pasta: Path):
    saida = ["-- Gerado por migrations/mongo_para_postgres.py", "BEGIN;", ""]

    # musicas (catálogo, sem dono)
    for d in ler_colecao(pasta, "musicas"):
        ano = numero(d.get("anoLancamento"))
        saida.append(inserir(
            "musicas",
            ["id", "nome", "artista", "album", "ano_lancamento", "preview_url", "capa_url", "uri", "source"],
            [oid(d.get("_id")), d.get("nome"), d.get("artista"), d.get("album"),
             int(ano) if ano is not None else None, d.get("previewUrl"), d.get("capaUrl"), d.get("uri"), d.get("source")],
            "(id)"))

    # playlists + faixas na ordem original
    for d in ler_colecao(pasta, "playlists"):
        pid, uid = oid(d.get("_id")), int(numero(d.get("userId")))
        saida.append(inserir(
            "playlists",
            ["id", "user_id", "nome", "vibe", "publico", "capa_url", "sync_spotify", "spotify_playlist_id",
             "bloqueada_moderacao", "bloqueio_motivo"],
            [pid, uid, d.get("nome"), d.get("vibe"), bool(d.get("publico", False)), d.get("capaUrl"),
             bool(d.get("syncSpotify", False)), d.get("spotifyPlaylistId"), d.get("bloqueadaModeracao"),
             d.get("bloqueioMotivo")],
            "(id)", exige_usuario=uid))
        for posicao, track_id in enumerate(d.get("trackIds") or []):
            saida.append(inserir(
                "playlist_tracks", ["playlist_id", "posicao", "track_id"], [pid, posicao, str(track_id)],
                "(playlist_id, posicao)", exige_playlist=pid))

    # curtidas
    for d in ler_colecao(pasta, "user_songs"):
        uid = int(numero(d.get("userId")))
        saida.append(inserir(
            "user_songs",
            ["id", "user_id", "track_id", "source", "track_name", "artist_name", "album_name", "data_adicao"],
            [oid(d.get("_id")), uid, d.get("trackId"), d.get("source"), d.get("trackName"), d.get("artistName"),
             d.get("albumName"), data(d.get("dataAdicao"))],
            exige_usuario=uid))

    # avaliações de álbum
    for d in ler_colecao(pasta, "album_ratings"):
        uid = int(numero(d.get("userId")))
        nota = numero(d.get("nota"))
        oculto_por = numero(d.get("ocultoPor"))
        saida.append(inserir(
            "album_ratings",
            ["id", "user_id", "album_key", "album_name", "artista", "capa_url", "source", "nota", "titulo", "review",
             "criado_em", "atualizado_em", "oculto", "oculto_motivo", "oculto_por", "oculto_em"],
            [oid(d.get("_id")), uid, d.get("albumKey"), d.get("albumName"), d.get("artista"), d.get("capaUrl"),
             d.get("source"), float(nota) if nota is not None else None, d.get("titulo"), d.get("review"),
             data(d.get("criadoEm")), data(d.get("atualizadoEm")), d.get("oculto"), d.get("ocultoMotivo"),
             int(oculto_por) if oculto_por is not None else None, data(d.get("ocultoEm"))],
            exige_usuario=uid))

    # histórico: só a reprodução mais recente de cada (usuário, faixa)
    mais_recente = {}
    for d in ler_colecao(pasta, "historico_reproducao"):
        chave = (int(numero(d.get("userId"))), d.get("trackId"))
        quando = data(d.get("dataReproducao")) or ""
        if chave not in mais_recente or quando > (data(mais_recente[chave].get("dataReproducao")) or ""):
            mais_recente[chave] = d
    for (uid, track_id), d in mais_recente.items():
        saida.append(inserir(
            "historico_reproducao", ["id", "user_id", "track_id", "data_reproducao"],
            [oid(d.get("_id")), uid, track_id, data(d.get("dataReproducao"))],
            exige_usuario=uid))

    saida += [
        "",
        "COMMIT;",
        "",
        "-- Conferência",
        "SELECT 'musicas' AS tabela, COUNT(*) FROM musicas",
        "UNION ALL SELECT 'playlists', COUNT(*) FROM playlists",
        "UNION ALL SELECT 'playlist_tracks', COUNT(*) FROM playlist_tracks",
        "UNION ALL SELECT 'user_songs', COUNT(*) FROM user_songs",
        "UNION ALL SELECT 'album_ratings', COUNT(*) FROM album_ratings",
        "UNION ALL SELECT 'historico_reproducao', COUNT(*) FROM historico_reproducao;",
    ]
    return "\n".join(saida) + "\n"


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    sys.stdout.write(gerar(Path(sys.argv[1])))
