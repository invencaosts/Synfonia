-- Link curto de compartilhamento de avaliação (/a/{codigo}).
-- Código gerado pelo backend no primeiro compartilhamento; avaliações antigas ficam NULL até lá.
ALTER TABLE album_ratings ADD COLUMN IF NOT EXISTS codigo_curto VARCHAR(16);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_album_ratings_codigo_curto') THEN
        ALTER TABLE album_ratings ADD CONSTRAINT uk_album_ratings_codigo_curto UNIQUE (codigo_curto);
    END IF;
END $$;
