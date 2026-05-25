# Gabarita+ Backend

Backend da plataforma inteligente de estudos `Gabarita+`, construído com `Java 21`, `Spring Boot 3`, `PostgreSQL`, `JWT`, `Flyway`, `Docker` e arquitetura em camadas.

## Stack

- `Java 21`
- `Spring Boot 3`
- `Spring Security`
- `JWT`
- `PostgreSQL`
- `Spring Data JPA`
- `Flyway`
- `Redis` preparado
- `Swagger / OpenAPI`
- `Docker` e `Docker Compose`

## Arquitetura

Package base:

`com.gabaritaplus.api`

Estrutura principal:

- `config`
- `controller`
- `dto`
- `entity`
- `exception`
- `mapper`
- `repository`
- `security`
- `service`
- `specification`
- `util`

## Funcionalidades implementadas

- Autenticacao com registro, login, refresh token e logout
- Controle de acesso com `ROLE_USER` e `ROLE_ADMIN`
- CRUD completo de questoes
- Filtros avancados de questoes com `Specification`
- Registro de respostas do usuario
- Caderno de erros com estrutura inicial para repeticao espacada
- Favoritos
- Dashboard com metricas consolidadas
- Criacao e finalizacao de simulados
- Migrations com Flyway
- Seeds iniciais
- Swagger/OpenAPI
- CORS e rate limit preparados
- Redis preparado para cache futuro

## Variaveis de ambiente

Use o arquivo `.env.example` como base.

Principais variaveis:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION_MINUTES`
- `JWT_REFRESH_EXPIRATION_DAYS`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_ENABLED`
- `RATE_LIMIT_REQUESTS_PER_MINUTE`
- `REDIS_ENABLED`

## Como rodar localmente

Requisitos:

- `Java 21`
- `Maven 3.9+`
- `PostgreSQL 16+`

Passos:

1. Crie um banco `gabaritaplus` no PostgreSQL.
2. Ajuste as variaveis de ambiente com base no `.env.example`.
3. Execute:

```bash
mvn clean spring-boot:run
```

Ou:

```bash
mvn clean package
java -jar target/gabaritaplus-api-1.0.0.jar
```

## Como rodar com Docker

Na pasta `backend/`, execute:

```bash
docker compose up --build
```

Para subir em background:

```bash
docker compose up -d --build
```

Para derrubar:

```bash
docker compose down
```

## Swagger

Com a aplicacao em execucao:

- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

## Seeds iniciais

Usuarios criados automaticamente:

- Admin
  - email: `admin@gabaritaplus.com`
  - username: `admin`
  - senha: `Admin@123`
- Usuario padrao
  - email: `user@gabaritaplus.com`
  - username: `aluno`
  - senha: `User@123`

Tambem sao criadas questoes de exemplo para validacao inicial da API.

## Endpoints principais

Autenticacao:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Usuario:

- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/users/me/statistics`

Questoes:

- `GET /api/questions`
- `GET /api/questions/{id}`
- `POST /api/questions`
- `PUT /api/questions/{id}`
- `DELETE /api/questions/{id}`
- `POST /api/questions/answers`
- `POST /api/questions/{id}/favorite`
- `GET /api/questions/error-notebook`

Dashboard:

- `GET /api/dashboard`

Simulados:

- `POST /api/mock-exams`
- `GET /api/mock-exams`
- `GET /api/mock-exams/{id}`
- `POST /api/mock-exams/{id}/finish`

## Exemplo de login

```json
{
  "usernameOrEmail": "admin@gabaritaplus.com",
  "password": "Admin@123"
}
```

## Exemplo de criacao de questao

```json
{
  "title": "Probabilidade basica",
  "statement": "Uma moeda honesta e lancada duas vezes. Qual a probabilidade de sair duas caras?",
  "subject": "Matematica",
  "topic": "Probabilidade",
  "subtopic": "Eventos independentes",
  "difficulty": "EASY",
  "year": 2024,
  "exam": "ENEM Regular",
  "competency": "Competencia 7",
  "ability": "Habilidade 28",
  "explanation": "A probabilidade e 1/2 x 1/2 = 1/4.",
  "correctAlternative": "A",
  "alternatives": [
    { "letter": "A", "text": "1/4", "correct": true },
    { "letter": "B", "text": "1/3", "correct": false },
    { "letter": "C", "text": "1/2", "correct": false },
    { "letter": "D", "text": "2/3", "correct": false },
    { "letter": "E", "text": "3/4", "correct": false }
  ]
}
```

## Observacoes

- O ambiente desta sessao nao possui `Maven` no `PATH` e esta com `Java 8`, entao a compilacao real nao pode ser validada aqui.
- O projeto foi estruturado para `Java 21` e deve ser executado em um ambiente compativel.
