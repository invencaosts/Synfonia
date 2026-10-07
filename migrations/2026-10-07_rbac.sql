-- RBAC: papéis por usuário, suspensão pela moderação e auditoria.
-- Papéis e permissões são definidos no código (RoleSistema/Permissao); aqui só fica quem tem qual papel.
-- Rodar ANTES de subir o backend novo (spring.jpa.hibernate.ddl-auto=validate). Idempotente.

-- Papéis elevados de cada usuário (USER é implícito e não é gravado)
CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    role       VARCHAR(32) NOT NULL CHECK (role IN ('MODERATOR', 'ADMIN', 'SUPER_ADMIN')),
    PRIMARY KEY (usuario_id, role)
);

-- Suspensão pela moderação (independente de ativo=false, que é a desativação pelo próprio usuário)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS banido        boolean DEFAULT false;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS banido_ate    timestamp(6);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS banido_motivo varchar(500);
UPDATE usuarios SET banido = false WHERE banido IS NULL;

-- Log imutável de ações de moderação
CREATE TABLE IF NOT EXISTS auditoria_moderacao (
    id              BIGSERIAL PRIMARY KEY,
    ator_id         BIGINT,
    acao            VARCHAR(64)  NOT NULL,
    alvo_usuario_id BIGINT,
    alvo_tipo       VARCHAR(32),
    alvo_id         VARCHAR(64),
    motivo          VARCHAR(500),
    detalhe         TEXT,
    criado_em       TIMESTAMP(6) NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_auditoria_criado_em     ON auditoria_moderacao (criado_em);
CREATE INDEX IF NOT EXISTS idx_auditoria_alvo_usuario  ON auditoria_moderacao (alvo_usuario_id);

-- A antiga coluna "papel" não concede mais acesso nenhum. Ela podia ser obtida cadastrando o e-mail
-- de ADMIN_USERNAME; confira quem tinha ADMIN (papel = 0) antes de conceder qualquer papel novo:
--   SELECT id, username, email, data_criacao FROM usuarios WHERE papel = 0;
