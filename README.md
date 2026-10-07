# 🎙️ Budgeting AI: API inteligente de orçamento com reconhecimento de fala

Projeto do desafio **"Desenvolvendo sua API Inteligente com Reconhecimento de Fala e Spring Boot"**, do bootcamp **Itaú – Java com Inteligência Artificial** (DIO).

Ele parte do projeto final do expert ([05-spring-ai](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai)) e acrescenta algumas evoluções, descritas abaixo.

## O que o projeto faz

É uma API de controle de gastos que entende **comandos de voz**. Você grava algo como *"Gastei 80 reais no mercado"* e a API:

1. recebe o arquivo de áudio;
2. transcreve o áudio em texto (**Whisper**, via `TranscriptionModel`);
3. envia o texto para um modelo de linguagem (**gpt-4o-mini**, via `ChatClient`);
4. o modelo escolhe e executa uma **ferramenta real** da aplicação (**Tool Calling**): registrar um gasto, listar por categoria ou resumir os gastos;
5. a transação é gravada ou consultada no **MySQL**;
6. a resposta final é convertida em áudio MP3 (**Text-to-Speech**) e devolvida ao cliente.

```
áudio ──► transcrição ──► ChatClient + Tools ──► casos de uso ──► MySQL
                                   │
resposta MP3 ◄── text-to-speech ◄──┘
```

## Melhorias implementadas

| # | Melhoria | Onde |
|---|----------|------|
| 1 | **Validações de domínio** antes de salvar: descrição obrigatória (até 255 caracteres), valor maior que zero e categoria obrigatória. Valem tanto para a API REST quanto para o Tool Calling, porque ficam na entidade `Transaction`. | `domain/Transaction.java`, `domain/InvalidTransactionException.java` |
| 2 | **Erros HTTP 400 padronizados** (Problem Details, RFC 9457) para transação inválida, categoria inexistente e requisição vazia, em vez de erro 500. | `infrastructure/http/error/ApiExceptionHandler.java` |
| 3 | **Nova ferramenta para a IA: `summarize-expenses`**, que devolve o total gasto e o total por categoria. Agora o assistente responde perguntas como *"quanto eu gastei no total?"* e *"onde gastei mais?"*. | `application/SummarizeExpensesUseCase.java` |
| 4 | **Novo endpoint `GET /transactions/summary`**, que usa o mesmo caso de uso. | `TransactionController.java` |
| 5 | **Novo endpoint `POST /transactions/ai/text`**: o mesmo fluxo de IA, mas com texto, o que facilita testar o Tool Calling sem gravar áudio. | `TransactionController.java` |
| 6 | **Correção da conversão de valores**: o domínio guarda o valor em centavos, mas a saída mostrava os centavos como se fossem reais (8000 centavos apareciam como `8000.0`). Agora a saída mostra reais (`80.0`). | `application/output/TransactionOutput.java` |
| 7 | **Prompt de sistema melhorado**: explica as categorias, a conversão reais → centavos e pede respostas curtas, próprias para áudio. | `resources/prompts/system-message.st` |
| 8 | **Novas categorias**: `RESTAURANT`, `LEISURE` e `OTHER`. | `domain/Category.java` |
| 9 | **Orquestração da IA extraída do controller** para a classe `BudgetAssistant`. O controller passa a cuidar só de HTTP. | `infrastructure/ai/BudgetAssistant.java` |
| 10 | **Testes automatizados** que rodam **sem chave da OpenAI e sem banco**: domínio, casos de uso e endpoints (MockMvc, com o assistente mockado). | `src/test/java/...` |
| 11 | **Configuração do VS Code** (extensões recomendadas e launch com `.env`) e arquivo **`requests.http`** com exemplos prontos. | `.vscode/`, `requests.http` |

## Tecnologias

- Java 25
- Spring Boot 4.0.5 (Web, Data JPA, Docker Compose)
- Spring AI 2.0.0-M4 com OpenAI (gpt-4o-mini, whisper-1, gpt-4o-mini-tts)
- MySQL 9.6 (via Docker Compose)
- Gradle, Lombok
- JUnit 5, AssertJ, Mockito, MockMvc

## Arquitetura

O projeto segue as camadas usadas em toda a trilha (DDD e Clean Architecture):

```
src/main/java/dio/budgeting
├── domain/            # Transaction, Category, regras de negócio e contrato do repositório
├── application/       # Casos de uso (também expostos como @Tool para a IA)
│   ├── PersistTransactionUseCase         -> tool "persist-transaction"
│   ├── ListTransactionsByCategoryUseCase -> tool "list-transactions-by-category"
│   └── SummarizeExpensesUseCase          -> tool "summarize-expenses" (novo)
└── infrastructure/
    ├── ai/            # BudgetAssistant: transcrição, ChatClient com tools e TTS (novo)
    ├── http/          # Controller, requests/responses e tratamento de erros
    └── persistence/   # Entidade e repositórios JPA
```

A IA não acessa o banco diretamente: ela só chama os **casos de uso**, os mesmos que a API REST usa. Por isso as validações de domínio protegem os dois caminhos.

## Como executar no VS Code

### Pré-requisitos

- **JDK 25**
- **Docker Desktop** em execução (o Spring Boot sobe o MySQL sozinho a partir do `compose.yml`)
- **Chave da OpenAI** com créditos
- VS Code com as extensões recomendadas (ao abrir a pasta, o VS Code sugere instalar: Java Pack, Spring Boot, Gradle, REST Client e Docker)

### Passo a passo

1. Clone o repositório e abra a pasta no VS Code:
   ```bash
   git clone <url-do-seu-repositorio>
   cd <pasta-do-projeto>
   code .
   ```
2. Crie o arquivo `.env` a partir do exemplo e coloque sua chave:
   ```bash
   cp .env.example .env
   # edite o .env: OPENAI_API_KEY=sk-...
   ```
3. Rode a aplicação:
   - **Pelo VS Code:** suba o banco antes com `docker compose up -d` e depois use a aba *Run and Debug* → **BudgetingApplication** (o launch já lê o `.env`), ou
   - **Pelo terminal:**
     ```bash
     export OPENAI_API_KEY="sua_chave"     # Windows PowerShell: $env:OPENAI_API_KEY="sua_chave"
     ./gradlew bootRun                     # Windows: .\gradlew.bat bootRun
     ```
4. A API sobe em `http://localhost:8080`, e o MySQL é iniciado automaticamente pelo Docker Compose (porta 3307).

## Como testar o fluxo principal

### Pelo VS Code (REST Client)

Abra o arquivo **`requests.http`** e clique em **Send Request** acima de cada requisição.

### Pelo terminal (curl)

**Comando de voz** (usa um dos áudios de exemplo do projeto e salva a resposta falada):

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```

**Comando em texto** (novo endpoint):

```bash
curl -X POST http://localhost:8080/transactions/ai/text \
  -H "Content-Type: application/json" \
  -d '{"message": "Gastei 60 reais no restaurante hoje"}'
```

Exemplo de resposta:

```json
{
  "message": "Gastei 60 reais no restaurante hoje",
  "answer": "Pronto! Registrei um gasto de 60 reais na categoria restaurante."
}
```

**Resumo de gastos** (novo endpoint):

```bash
curl http://localhost:8080/transactions/summary
```

```json
{
  "total": 360.0,
  "transactionCount": 4,
  "categories": [
    { "category": "AUTO", "total": 200.0, "transactionCount": 1 },
    { "category": "GROCERIES", "total": 120.0, "transactionCount": 2 },
    { "category": "PHARMA", "total": 40.0, "transactionCount": 1 }
  ]
}
```

**Validação** (novo comportamento):

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Mercado", "category": "GROCERIES", "amount": 0}'
```

```json
{
  "type": "about:blank",
  "title": "Transação inválida",
  "status": 400,
  "detail": "O valor da transação deve ser maior que zero.",
  "instance": "/transactions"
}
```

### Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/transactions` | Cria uma transação (`amount` em centavos) |
| `GET` | `/transactions/{category}` | Lista as transações de uma categoria |
| `GET` | `/transactions/summary` | **Novo:** resumo de gastos por categoria |
| `POST` | `/transactions/ai` | Comando de voz (multipart `file`), devolve um MP3 |
| `POST` | `/transactions/ai/text` | **Novo:** comando em texto, devolve JSON |

Categorias: `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `LEISURE` e `OTHER`.

## Testes automatizados

```bash
./gradlew test
```

- **Rodam sempre**, sem chave e sem banco: `TransactionTest` (validações de domínio), `TransactionUseCasesTest` (casos de uso e ferramentas da IA) e `TransactionControllerTest` (endpoints com MockMvc).
- **Rodam só com `OPENAI_API_KEY` definida** (e com o MySQL de pé, via `docker compose up -d`): os testes de integração com a OpenAI (`*IT.java`) e o `contextLoads`.

## O que aprendi

> ✏️ *Rascunho: personalize com as suas palavras antes de entregar.*

- Como o **Spring AI** abstrai os provedores de IA (`ChatModel`, `ChatClient`, `TranscriptionModel`, `TextToSpeechModel`), o que permite trocar de modelo apenas pela configuração.
- Como o **Tool Calling** liga a IA a regras de negócio reais: o modelo decide *qual* caso de uso chamar, mas quem executa e valida é o código da aplicação.
- Por que vale a pena colocar as **validações no domínio**: a mesma regra protege a API REST e as ações da IA.
- Que o **prompt de sistema** faz parte do código: detalhes como "valores em centavos" mudam o resultado.
- Como testar uma aplicação com IA **separando o que é determinístico** (domínio, casos de uso, endpoints) do que depende do modelo (testes de integração).

## Créditos

Projeto base: [digitalinnovationone/dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track), módulo `05-spring-ai`, apresentado pelo expert Poiani na DIO.
