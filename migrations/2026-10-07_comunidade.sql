-- Comunidade: privacidade do perfil + índices de leitura.
-- Rodar ANTES de subir o backend novo (spring.jpa.hibernate.ddl-auto=validate).
-- Idempotente: pode ser executado mais de uma vez.

-- ===== PostgreSQL (db_usuarios_musicas) =====
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS perfil_publico  boolean DEFAULT true;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS show_curtidas   boolean DEFAULT true;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS show_avaliacoes boolean DEFAULT true;

UPDATE usuarios SET perfil_publico  = true WHERE perfil_publico  IS NULL;
UPDATE usuarios SET show_curtidas   = true WHERE show_curtidas   IS NULL;
UPDATE usuarios SET show_avaliacoes = true WHERE show_avaliacoes IS NULL;

-- Busca por username/displayName sem diferenciar maiúsculas
CREATE INDEX IF NOT EXISTS idx_usuarios_username_lower ON usuarios (LOWER(username));

-- (Os índices de avaliações, curtidas e playlists ficam em 2026-10-07_mongo_para_postgres.sql,
--  já que essas tabelas saíram do MongoDB.)
