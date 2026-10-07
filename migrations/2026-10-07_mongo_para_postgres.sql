-- Fim do MongoDB: catálogo, playlists, curtidas, avaliações e histórico passam a morar no PostgreSQL.
-- Rodar ANTES de subir o backend novo (spring.jpa.hibernate.ddl-auto=validate). Idempotente.
-- Depois desta migration, importe os dados do Mongo com migrations/mongo_para_postgres.py (ver LEIA-ME.md).
--
-- Toda tabela com dono tem FK para usuarios com ON DELETE CASCADE: excluir a conta apaga os dados dela
-- (antes, no Mongo, eles ficavam órfãos para sempre).

-- Catálogo local (cache das fontes externas); id = id da faixa na fonte
CREATE TABLE IF NOT EXISTS musicas (
    id             VARCHAR(128) PRIMARY KEY,
    nome           TEXT,
    artista        TEXT,
    album          TEXT,
    ano_lancamento INTEGER,
    preview_url    TEXT,
    capa_url       TEXT,
    uri            TEXT,
    source         VARCHAR(32) CHECK (source IN ('SPOTIFY', 'ITUNES', 'YOUTUBE_MUSIC'))
);
CREATE INDEX IF NOT EXISTS idx_musicas_nome    ON musicas (nome);
CREATE INDEX IF NOT EXISTS idx_musicas_artista ON musicas (artista);

-- ids em texto: registros migrados mantêm o ObjectId do Mongo, novos recebem UUID
CREATE TABLE IF NOT EXISTS playlists (
    id                  VARCHAR(64) PRIMARY KEY,
    user_id             BIGINT  NOT NULL,
    nome                TEXT,
    vibe                VARCHAR(64),
    publico             BOOLEAN NOT NULL DEFAULT false,
    capa_url            TEXT,
    sync_spotify        BOOLEAN NOT NULL DEFAULT false,
    spotify_playlist_id VARCHAR(128),
    bloqueada_moderacao BOOLEAN,
    bloqueio_motivo     VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_playlists_user_publico ON playlists (user_id, publico);

-- Faixas da playlist na ordem (posicao 0..n-1)
CREATE TABLE IF NOT EXISTS playlist_tracks (
    playlist_id VARCHAR(64)  NOT NULL,
    posicao     INTEGER      NOT NULL,
    track_id    VARCHAR(128) NOT NULL,
    PRIMARY KEY (playlist_id, posicao)
);

CREATE TABLE IF NOT EXISTS user_songs (
    id          VARCHAR(64)  PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    track_id    VARCHAR(128) NOT NULL,
    source      VARCHAR(32) CHECK (source IN ('SPOTIFY', 'ITUNES', 'YOUTUBE_MUSIC')),
    track_name  TEXT,
    artist_name TEXT,
    album_name  TEXT,
    data_adicao TIMESTAMP(6),
    CONSTRAINT uk_user_songs_usuario_faixa UNIQUE (user_id, track_id)
);
CREATE INDEX IF NOT EXISTS idx_user_songs_usuario_data ON user_songs (user_id, data_adicao);

CREATE TABLE IF NOT EXISTS album_ratings (
    id            VARCHAR(64)  PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    album_key     VARCHAR(512) NOT NULL,
    album_name    TEXT,
    artista       TEXT,
    capa_url      TEXT,
    source        VARCHAR(32) CHECK (source IN ('SPOTIFY', 'ITUNES', 'YOUTUBE_MUSIC')),
    nota          DOUBLE PRECISION,
    titulo        VARCHAR(200),
    review        TEXT,
    criado_em     TIMESTAMP(6),
    atualizado_em TIMESTAMP(6),
    oculto        BOOLEAN,
    oculto_motivo VARCHAR(500),
    oculto_por    BIGINT,
    oculto_em     TIMESTAMP(6),
    CONSTRAINT uk_album_ratings_usuario_album UNIQUE (user_id, album_key)
);
CREATE INDEX IF NOT EXISTS idx_album_ratings_usuario_data ON album_ratings (user_id, atualizado_em);

-- Uma linha por (usuário, faixa): tocar de novo atualiza data_reproducao
CREATE TABLE IF NOT EXISTS historico_reproducao (
    id              VARCHAR(64)  PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    track_id        VARCHAR(128) NOT NULL,
    data_reproducao TIMESTAMP(6),
    CONSTRAINT uk_historico_usuario_faixa UNIQUE (user_id, track_id)
);
CREATE INDEX IF NOT EXISTS idx_historico_usuario_data ON historico_reproducao (user_id, data_reproducao);

-- Chaves estrangeiras com cascata (recriadas para garantir o ON DELETE CASCADE mesmo se o
-- Hibernate de dev já tiver criado a FK sem ele)
ALTER TABLE playlists            DROP CONSTRAINT IF EXISTS fk_playlists_usuario;
ALTER TABLE playlists            ADD  CONSTRAINT fk_playlists_usuario
    FOREIGN KEY (user_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE playlist_tracks      DROP CONSTRAINT IF EXISTS fk_playlist_tracks_playlist;
ALTER TABLE playlist_tracks      ADD  CONSTRAINT fk_playlist_tracks_playlist
    FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE;
ALTER TABLE user_songs           DROP CONSTRAINT IF EXISTS fk_user_songs_usuario;
ALTER TABLE user_songs           ADD  CONSTRAINT fk_user_songs_usuario
    FOREIGN KEY (user_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE album_ratings        DROP CONSTRAINT IF EXISTS fk_album_ratings_usuario;
ALTER TABLE album_ratings        ADD  CONSTRAINT fk_album_ratings_usuario
    FOREIGN KEY (user_id) REFERENCES usuarios (id) ON DELETE CASCADE;
ALTER TABLE historico_reproducao DROP CONSTRAINT IF EXISTS fk_historico_usuario;
ALTER TABLE historico_reproducao ADD  CONSTRAINT fk_historico_usuario
    FOREIGN KEY (user_id) REFERENCES usuarios (id) ON DELETE CASCADE;

-- Papéis do usuário também caem junto com a conta
ALTER TABLE usuario_roles DROP CONSTRAINT IF EXISTS fk_usuario_roles_usuario;
DO $$
DECLARE fk TEXT;
BEGIN
    -- FK gerada pelo Hibernate em dev tem nome aleatório; remove qualquer FK de usuario_roles -> usuarios
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'usuario_roles'::regclass AND contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE usuario_roles DROP CONSTRAINT %I', fk);
    END LOOP;
END $$;
ALTER TABLE usuario_roles ADD CONSTRAINT fk_usuario_roles_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;

-- Outras tabelas ligadas à conta (problemas anteriores à migração):
-- password_reset_tokens tinha FK sem cascata (excluir conta com token pendente falhava) e
-- ytmusic_credentials não tinha FK nenhuma (credenciais de contas excluídas ficavam órfãs).
DO $$
DECLARE fk TEXT;
BEGIN
    FOR fk IN SELECT conname FROM pg_constraint
              WHERE conrelid = 'password_reset_tokens'::regclass AND contype = 'f'
                AND confrelid = 'usuarios'::regclass
    LOOP
        EXECUTE format('ALTER TABLE password_reset_tokens DROP CONSTRAINT %I', fk);
    END LOOP;
END $$;
ALTER TABLE password_reset_tokens ADD CONSTRAINT fk_password_reset_tokens_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;

DELETE FROM ytmusic_credentials c WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.id = c.usuario_id);
ALTER TABLE ytmusic_credentials DROP CONSTRAINT IF EXISTS fk_ytmusic_credentials_usuario;
ALTER TABLE ytmusic_credentials ADD CONSTRAINT fk_ytmusic_credentials_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE;
