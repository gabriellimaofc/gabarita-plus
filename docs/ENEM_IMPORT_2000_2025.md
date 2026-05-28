# ENEM Import 2000-2025

## Objetivo

Preparar o Gabarita+ para receber apenas questoes oficiais do ENEM, rastreaveis ate fontes do INEP, sem inventar conteudo e sem usar plataformas terceiras.

## Fontes permitidas

- Provas e gabaritos oficiais do INEP
- Microdados oficiais do ENEM publicados pelo INEP

Nao usar sites de cursinho, bancos privados, PDFs reupados por terceiros ou conteudo gerado por IA.

## Organizacao recomendada

Estruture os arquivos brutos por ano, dia e caderno.

```text
raw/enem/
  2024/
    dia-1/
      azul/
        prova.pdf
        gabarito.pdf
    dia-2/
      azul/
        prova.pdf
        gabarito.pdf
```

Os JSONs preparados para importacao devem seguir a mesma logica.

```text
prepared/enem/
  2024/
    dia-2/
      azul/
        enem-2024-dia-2-azul.json
```

## Campos de rastreabilidade

Cada questao precisa carregar:

- `source`
- `sourceUrl`
- `sourceExam`
- `sourceYear`
- `sourceQuestionNumber`
- `sourceBookColor`
- `sourceDay`
- `sourcePage`
- `statementHash`
- `importBatchId`
- `importStatus`

Isso permite auditar origem, recarregar lotes e bloquear duplicidade entre cores de caderno.

## Regra de duplicidade

Nao importar a mesma questao varias vezes por causa da cor do caderno.

O pipeline usa:

- identidade oficial: `sourceExam + sourceYear + sourceQuestionNumber + sourceDay + sourceBookColor`
- hash do enunciado: `statementHash`

Se a identidade ou o hash ja existirem para o mesmo contexto oficial, a questao deve ser ignorada como duplicada.

## Formato de importacao

Use o schema em [database/examples/enem-official-import.schema.json](/C:/Users/studioorla/Desktop/gabarita-plus/database/examples/enem-official-import.schema.json) e o exemplo placeholder em [database/examples/enem-official-import.example.json](/C:/Users/studioorla/Desktop/gabarita-plus/database/examples/enem-official-import.example.json).

O exemplo usa placeholders de estrutura. Ele nao traz questoes reais nem ficticias.

## Assets e completude

Questao oficial com grafico, mapa, charge, tabela, formula, diagrama ou imagem so pode entrar como completa se os assets estiverem vinculados.

Quando o texto indicar referencia visual e o asset nao estiver presente:

- `importStatus = NEEDS_REVIEW`
- nao publicar automaticamente

Quando houver erro estrutural claro:

- menos de 5 alternativas
- letras fora de `A-E`
- gabarito ausente
- alternativa vazia

Entao:

- `importStatus = INVALID`

## Status de importacao

- `VALIDATED`: estrutura valida e sem sinais de incompletude
- `NEEDS_REVIEW`: ha duvida de completude ou texto quebrado
- `INVALID`: estrutura quebrada
- `PUBLISHED`: visivel para alunos

Somente `PUBLISHED` deve aparecer para alunos.

## Dry-run

Antes de qualquer importacao real, rode validacao local e depois o endpoint administrativo de dry-run.

Exemplos locais:

```bash
node tools/import-enem/validate-json.mjs prepared/enem/2024/dia-2/azul/enem-2024-dia-2-azul.json
node tools/import-enem/check-duplicates.mjs prepared/enem/2024/dia-2/azul/enem-2024-dia-2-azul.json
node tools/import-enem/detect-incomplete.mjs prepared/enem/2024/dia-2/azul/enem-2024-dia-2-azul.json
```

Dry-run no backend:

```http
POST /api/admin/import/questions/dry-run
Authorization: Bearer <jwt-admin>
Content-Type: application/json
```

Script PowerShell para validar o dry-run em producao sem persistir dados:

```powershell
cd backend
.\scripts\test-import-dry-run.ps1
```

O script:

- faz login como admin
- carrega `database/examples/enem-official-import.example.json`
- envia o payload para `POST /api/admin/import/questions/dry-run`
- mostra o relatorio de validacao
- compara a quantidade de `import_batches` antes e depois
- exige `batchId = null` no retorno
- confirma explicitamente que nada foi salvo no banco

Parametros uteis:

```powershell
.\scripts\test-import-dry-run.ps1 `
  -ApiBaseUrl "https://gabarita-plus-api.onrender.com/api" `
  -AdminEmail "admin@gabaritaplus.com" `
  -ImportFile "..\..\database\examples\enem-official-import.example.json"
```

O script nao imprime senha, access token completo, refresh token, JWT secreto ou credenciais do banco.

## Importacao real

JSON:

```http
POST /api/admin/import/questions/json
Authorization: Bearer <jwt-admin>
Content-Type: multipart/form-data
file=@enem-2024-dia-2-azul.json
```

CSV:

```http
POST /api/admin/import/questions/csv
Authorization: Bearer <jwt-admin>
Content-Type: multipart/form-data
file=@enem-2024-dia-2-azul.csv
```

No CSV, a coluna `alternatives` deve conter um JSON serializado com as cinco alternativas. A coluna `assets` recebe um JSON serializado com os assets do enunciado.

## Assets e storage

O backend esta preparado para:

- storage local em desenvolvimento
- migracao futura para Supabase Storage

Variaveis reservadas:

- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SUPABASE_BUCKET_QUESTIONS`

Nunca exponha `SUPABASE_SERVICE_ROLE_KEY` no frontend.

## Pipeline futuro de PDF

Arquitetura preparada para os proximos passos:

1. baixar PDF oficial do INEP
2. separar por ano, dia e caderno
3. extrair texto
4. detectar blocos de questao
5. detectar imagens, tabelas e graficos
6. recortar assets
7. enviar assets ao storage
8. associar assets a questao e alternativa corretas
9. aplicar gabarito oficial
10. gerar JSON de importacao
11. rodar validacao local
12. rodar dry-run
13. revisar `NEEDS_REVIEW`
14. publicar apenas `PUBLISHED`

## Auditoria

Para auditar uma questao importada, confira:

- `sourceUrl`
- `sourcePage`
- `sourceQuestionNumber`
- `sourceBookColor`
- `sourceDay`
- `statementHash`
- `importBatchId`

## Por que nao usamos terceiros

- terceiros podem alterar ordem, texto ou imagens
- terceiros podem remover elementos visuais
- terceiros reduzem a rastreabilidade
- a base precisa ser juridicamente e tecnicamente auditavel

## Principio operacional

Importar menos questoes, mas completas e rastreaveis, e melhor do que publicar uma base grande com itens quebrados.
