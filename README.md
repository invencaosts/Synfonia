# 🎵 Synfonia — Backend

API que dá vida ao [Synfonia](https://github.com/invencaosts/Synfonia-Front): autenticação, perfis, playlists, integração com Spotify e YouTube Music, avaliação de álbuns e histórico de reprodução.

**[📲 Baixar o app](https://github.com/invencaosts/Synfonia-Front/releases/latest)** · Frontend: [`invencaosts/Synfonia-Front`](https://github.com/invencaosts/Synfonia-Front)

---

## Stack

- **Java 21 + Spring Boot 3** — API REST com Spring Security (JWT + cookies HttpOnly)
- **PostgreSQL** — usuários, avaliações, dados relacionais
- **MongoDB** — músicas, playlists, histórico
- **Docker Compose** — sobe os bancos com um comando, sem SQL manual

## Segurança

Rate limiting (Bucket4j), proteção contra IDOR em endpoints sensíveis, cookies HttpOnly, zero log de credenciais.

---

## Rodando local

```bash
cp .env.example .env   # configure as variáveis
docker compose up -d --wait
./gradlew bootRun
```

API sobe em `http://localhost:8080`, Swagger em `/swagger-ui/index.html`.
