# Migrations do Synfonia

Não há Flyway: os scripts são aplicados à mão, **antes** de subir o backend novo.
Em produção o Hibernate roda com `ddl-auto=validate` e o app **não sobe** se faltar tabela ou coluna.
Todos os scripts são idempotentes (podem rodar mais de uma vez).

## Instalação nova (banco vazio)

Rode só o schema completo e suba o app; o seed cria as contas fixas a partir de `SEED_*`:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f 2026-10-07_00_schema_base.sql
```

## Atualização de um banco existente — ordem

| # | Arquivo | O que faz |
|---|---------|-----------|
| 1 | `2026-10-07_comunidade.sql` | Privacidade do perfil (`perfil_publico`, `show_curtidas`, `show_avaliacoes`) |
| 2 | `2026-10-07_rbac.sql` | Papéis por usuário, suspensão e auditoria de moderação |
| 3 | `2026-10-07_mongo_para_postgres.sql` | Tabelas que saíram do MongoDB + FKs com `ON DELETE CASCADE` |
| 4 | `mongo_para_postgres.py` | Copia os dados do MongoDB para as tabelas do passo 3 (uma vez só) |
| 5 | `2026-10-08_link_curto_avaliacao.sql` | Coluna `codigo_curto` do link curto de avaliação (`/a/{codigo}`) |

## Roteiro de produção (fim do MongoDB)

1. **Backup dos dois bancos** antes de tudo:
   ```bash
   pg_dump "$DATABASE_URL" -Fc > backup_postgres.dump
   mongodump --uri "$MONGO_URI" --archive --gzip > backup_mongo.archive.gz
   ```
2. **Schema** (passos 1 a 3):
   ```bash
   for f in 2026-10-07_comunidade.sql 2026-10-07_rbac.sql 2026-10-07_mongo_para_postgres.sql; do
     psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$f"
   done
   ```
3. **Dados do Mongo** (passo 4). Exportar com a aplicação parada ou em manutenção, para não perder escritas:
   ```bash
   mkdir -p export
   for c in musicas playlists user_songs album_ratings historico_reproducao; do
     mongoexport --uri "$MONGO_URI" --collection "$c" --jsonFormat=relaxed --out "export/$c.json"
   done
   python3 mongo_para_postgres.py export/ > dados_mongo.sql
   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f dados_mongo.sql
   ```
   O final do `dados_mongo.sql` mostra a contagem por tabela; compare com o `countDocuments()` de cada
   coleção. A diferença esperada são só os registros órfãos (de contas que já não existem), que são descartados,
   e entradas repetidas do histórico (fica a mais recente de cada faixa).
4. **Variáveis de ambiente** na Azure (App Settings):
   - remover `MONGO_*`;
   - definir `SEED_SUPER_ADMIN_*`, `SEED_MODERATOR_*`, `SEED_USER_*` (EMAIL, USERNAME, PASSWORD) com valores próprios de produção.
5. **Deploy** do backend novo. Conferir no log: `Started MusicasApplication` e as linhas do `[UsuariosSeeder]`.
6. Com tudo validado por alguns dias, desligar o MongoDB de produção (o backup do passo 1 fica guardado).

## Como foi testado

O roteiro acima foi executado localmente contra um backup real (Postgres + Mongo): restauração em banco
temporário, migrations 1→3, importação, segunda importação sem duplicar, e o app subindo em modo `validate`
com leitura e escrita de playlists, curtidas, avaliações e histórico.
