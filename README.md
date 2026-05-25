# Gabarita+

[![Backend CI](https://github.com/gabriellimaofc/gabarita-plus/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/gabriellimaofc/gabarita-plus/actions/workflows/backend-ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)

Plataforma inteligente de estudos para ENEM, com foco em desempenho, organizacao, revisao inteligente e evolucao orientada por dados.

## Monorepo

O repositorio esta organizado para separar claramente as responsabilidades da plataforma:

- `frontend/`: aplicacao web
- `backend/`: API Spring Boot
- `database/`: artefatos e suporte de banco de dados
- `docs/`: documentacao do projeto

## Backend

O backend do `Gabarita+` fica em `backend/` e foi estruturado com uma base pronta para evolucao profissional.

Stack principal:

- Java 21
- Spring Boot 3
- Maven
- Spring Security + JWT
- PostgreSQL
- Flyway
- Docker

Recursos implementados:

- autenticacao com JWT e refresh token
- controle de acesso por roles
- CRUD de questoes
- respostas de usuario
- caderno de erros
- dashboard de desempenho
- simulados
- seeds iniciais
- Swagger/OpenAPI

## CI/CD

O projeto possui uma pipeline de integracao continua para o backend com GitHub Actions.

Arquivo do workflow:

- `.github/workflows/backend-ci.yml`

Comportamento da pipeline:

- executa em `push` e `pull_request`
- roda apenas quando houver alteracoes em `backend/**`
- usa `ubuntu-latest`
- configura `Java 21`
- habilita cache Maven
- entra automaticamente em `backend/`
- executa `mvn clean install`
- valida dependencias, compilacao e testes automatizados
- publica relatorios de teste como artifact

Essa estrutura deixa a esteira pronta para evoluir depois para CD, deploy automatizado, quality gates e validacoes extras.

## Como visualizar a pipeline

No GitHub:

1. Abra o repositorio `gabriellimaofc/gabarita-plus`
2. Clique na aba `Actions`
3. Abra o workflow `Backend CI`
4. Selecione uma execucao para ver jobs, steps e logs

## Como debugar falhas

Se a pipeline falhar:

1. Abra a execucao com erro na aba `Actions`
2. Entre no job `Build and Test Backend`
3. Expanda o step que falhou
4. Leia o log para identificar se o problema foi de compilacao, dependencia ou testes
5. Baixe o artifact `backend-test-reports` quando houver relatorios publicados

Sinais mais comuns:

- falha em compilacao: erro de codigo, imports ou incompatibilidade com Java 21
- falha em dependencias: problema no `pom.xml` ou resolucao Maven
- falha em testes: teste unitario/integracao quebrado

## Como reexecutar jobs

No GitHub Actions:

1. Abra a execucao da pipeline
2. Clique em `Re-run jobs`
3. Escolha entre reexecutar todos os jobs ou apenas os com falha

## Desenvolvimento local

Para detalhes do backend, configuracao local, Docker, Swagger e seeds, consulte:

- [backend/README.md](backend/README.md)

## Proximo passo natural

A esteira atual cobre CI do backend e ja esta pronta para evoluir para:

- validacao de qualidade com Checkstyle ou SpotBugs
- build de imagem Docker
- publicacao de artefatos
- deploy automatizado em ambiente de staging/producao
