# 🎵 Synfonia - Core System (Backend)

O **Synfonia** é uma plataforma de gerenciamento e descoberta musical de alto desempenho. Este repositório contém o **Core System**, desenvolvido com uma arquitetura robusta, segura e escalável utilizando as tecnologias mais modernas do ecossistema Java.

---

## 🚀 Tecnologias e Stack
- **Java 21 (LTS)**: Aproveitando as últimas melhorias de performance e sintaxe.
- **Spring Boot 3.3.4**: Base para uma aplicação resiliente e produtiva.
- **Arquitetura de Banco de Dados Híbrida**:
  - **PostgreSQL**: Persistência de dados relacionais (usuários, coleções, senhas).
  - **MongoDB**: Armazenamento de alta performance para metadados de músicas, históricos e caches.
- **Mapeamento & Produtividade**: MapStruct (geração de DTOs) e Lombok.
- **Segurança**: Spring Security com JWT e Cookies HttpOnly.

---

## 🛡️ Segurança e Hardening (Blindagem)
O projeto passou por um processo rigoroso de endurecimento de segurança:

1.  **Proteção contra IDOR (BOLA)**: Endpoints sensíveis (`/me`, `/songs`, `/playlists`) identificam o usuário via Contexto de Segurança (JWT), impedindo que um usuário acesse ou modifique dados de terceiros mudando IDs na URL.
2.  **Rate Limiting (Bucket4j)**: Proteção nativa contra ataques de força bruta e DDoS, limitando tentativas de login e acessos à API por IP.
3.  **Cookies HttpOnly & Secure**: Tokens de acesso são armazenados em cookies protegidos, mitigando ataques de XSS.
4.  **Credential Safety**: Política de log zero para dados sensíveis. Nenhuma senha ou token de recuperação é impresso nos logs do sistema.
5.  **Ambiente de Produção Confiável**: Configurações via variáveis de ambiente com `ddl-auto=validate`, garantindo que o banco de dados nunca seja corrompido acidentalmente.

---

## ✨ Funcionalidades Principais
- **Integração Spotify**: Importação de playlists e sincronização de metadados via Proxy seguro.
- **Gestão de Coleções**: Sistema completo de salvamento de músicas favoritas com categorização por fonte.
- **Recuperação de Senha**: Fluxo seguro com códigos de 6 dígitos enviados por e-mail e expiração temporária.
- **Histórico de Reprodução**: Rastreamento em tempo real das faixas ouvidas com persistência em MongoDB.

---

## 🛠️ Infraestrutura e CI/CD
- **Docker Compose**: Ambiente pronto para orquestrar PostgreSQL, MongoDB e SonarQube para análise de código.
- **GitHub Actions**: Pipeline de **Integração Contínua (CI)** que valida cada Pull Request, garantindo que o código na branch `main` sempre compile e passe nos testes.

---

## ⚙️ Como Executar Localmente
1. Certifique-se de ter o **JDK 21** instalado.
2. Configure as **Variáveis de Ambiente** conforme o arquivo `.env.example`.
3. Inicie os serviços de banco de dados via Docker:
   ```bash
   docker-compose up -d
   ```
4. Execute o projeto usando o Gradle Wrapper:
   ```bash
   ./gradlew bootRun
   ```

---
*Desenvolvido com foco em qualidade, segurança e experiência do usuário.*
