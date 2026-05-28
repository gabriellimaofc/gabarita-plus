# ENEM API Import Strategy

## Objetivo

Usar APIs publicas e open-source como acelerador operacional para montar lotes de importacao do ENEM, sem tratar essas APIs como fonte oficial de publicacao.

No Gabarita+:

- `enem.dev` e fonte auxiliar
- `INEP` continua sendo a fonte oficial
- nenhuma questao vinda de API externa e publicada automaticamente
- toda questao externa entra em `NEEDS_REVIEW` por padrao enquanto nao houver validacao oficial

## Principios de seguranca

- Nao inventamos questoes
- Nao publicamos automaticamente conteudo vindo de API comunitaria
- Nao removemos rastreabilidade de origem
- Nao substituimos a conferencia em PDF/gabarito oficial do INEP
- Nao dependemos para sempre de URLs externas de imagem

## Como a importacao funciona

1. Buscar anos disponiveis na API auxiliar.
2. Buscar as questoes de um ano especifico.
3. Normalizar para o schema interno do Gabarita+.
4. Gerar `statementHash`.
5. Detectar duplicidade por hash, identidade de origem e `externalQuestionId`.
6. Detectar risco de incompletude.
7. Rodar `dry-run`.
8. Revisar o relatorio.
9. Importar apenas se o admin solicitar.
10. Validar depois contra fonte oficial do INEP antes de publicar.

## Campos de auditoria

Cada questao externa pode registrar:

- `externalProvider`
- `externalProviderUrl`
- `externalQuestionId`
- `externalLicense`
- `officialSourceUrl`
- `officialPdfUrl`
- `officialAnswerKeyUrl`
- `officialPage`
- `validatedAgainstOfficialSource`
- `validatedAt`

Esses campos existem para auditoria, revisao e futura conciliacao com o INEP.

## Status esperado para API externa

Regras atuais:

- `source = ENEM_DEV`
- `sourceExam = ENEM`
- `sourceBookColor = UNKNOWN` quando a API nao informar
- `sourceDay = null` quando nao for possivel inferir
- `validatedAgainstOfficialSource = false` por padrao
- `importStatus = NEEDS_REVIEW` enquanto nao houver validacao oficial

Mesmo quando a estrutura da questao vier aparentemente completa, o fluxo de API externa nao publica automaticamente.

## Imagens, graficos e tabelas

Quando a questao menciona:

- grafico
- figura
- imagem
- mapa
- tabela
- charge
- tirinha
- esquema
- desenho
- ilustracao
- diagrama
- observe
- conforme mostrado
- a seguir

e nao houver asset confiavel associado, a questao permanece em `NEEDS_REVIEW`.

Quando a API externa trouxer URL de imagem:

- a URL e registrada como asset de origem
- a publicacao definitiva nao deve depender dessa URL para sempre
- a copia futura para Supabase Storage continua sendo recomendada

## Endpoints admin

Todos os endpoints exigem JWT com role `ADMIN`.

- `GET /admin/import/enem-dev/years`
- `GET /admin/import/enem-dev/preview?year=2023`
- `POST /admin/import/enem-dev/dry-run`
- `POST /admin/import/enem-dev/import`

## Scripts locais

Pasta:

`tools/import-enem/enem-dev/`

Scripts disponiveis:

- `fetch-year.mjs`
- `fetch-all-years.mjs`
- `normalize-enem-dev.mjs`
- `dry-run-enem-dev.mjs`
- `generate-review-report.mjs`

### Exemplo de uso

Baixar um ano:

```bash
node tools/import-enem/enem-dev/fetch-year.mjs 2023 --limit=20 --language=espanhol
```

Normalizar:

```bash
node tools/import-enem/enem-dev/normalize-enem-dev.mjs 2023
```

Gerar relatorio local:

```bash
node tools/import-enem/enem-dev/generate-review-report.mjs 2023
```

Rodar dry-run contra a API:

```bash
ADMIN_EMAIL=admin@gabaritaplus.com \
ADMIN_PASSWORD='***' \
API_BASE_URL=https://gabarita-plus-api.onrender.com/api \
node tools/import-enem/enem-dev/dry-run-enem-dev.mjs 2023
```

No PowerShell:

```powershell
$env:ADMIN_EMAIL="admin@gabaritaplus.com"
$env:ADMIN_PASSWORD="***"
$env:API_BASE_URL="https://gabarita-plus-api.onrender.com/api"
node tools/import-enem/enem-dev/dry-run-enem-dev.mjs 2023
```

O script:

- nao imprime a senha
- nao imprime o token completo
- salva o relatorio em `database/imports/enem-dev/<ano>/dry-run-report.json`

## Saidas locais

Para cada ano:

- `database/imports/enem-dev/<ano>/questions.raw.json`
- `database/imports/enem-dev/<ano>/questions.normalized.json`
- `database/imports/enem-dev/<ano>/review-report.md`
- `database/imports/enem-dev/<ano>/dry-run-report.json`

## Por que o INEP continua sendo obrigatorio

APIs comunitarias ajudam a acelerar:

- descoberta de anos
- extracao inicial
- pre-normalizacao
- montagem de lotes para revisao

Mas a publicacao oficial continua dependente de:

- prova oficial do INEP
- gabarito oficial do INEP
- revisao de completude
- revisao de assets
- rastreabilidade verificavel

## Cuidados com licenca

- nao remova a referencia ao provedor externo
- nao presuma licenca de conteudo se a API nao expor isso claramente
- use `externalLicense` apenas quando essa informacao estiver disponivel e auditavel
- trate a licenca do software da API e a licenca do conteudo como coisas diferentes
