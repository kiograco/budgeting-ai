package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.http.request.AssistantTextRequest;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.AssistantTextResponse;
import dio.budgeting.infrastructure.http.response.ExpenseSummaryResponse;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final SummarizeExpensesUseCase summarizeExpensesUseCase;
    private final BudgetAssistant budgetAssistant;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 SummarizeExpensesUseCase summarizeExpensesUseCase,
                                 BudgetAssistant budgetAssistant) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.summarizeExpensesUseCase = summarizeExpensesUseCase;
        this.budgetAssistant = budgetAssistant;
    }

    /**
     * Cria uma transação. O campo {@code amount} do corpo é em centavos (ex.: 8000 = R$ 80,00).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(request.toInput());
        return TransactionResponse.from(transaction);
    }

    /**
     * Novo endpoint: resumo de gastos (total geral e por categoria).
     * Rotas literais têm prioridade sobre {@code /{category}} no Spring MVC.
     */
    @GetMapping("/summary")
    public ExpenseSummaryResponse summarizeExpenses() {
        return ExpenseSummaryResponse.from(summarizeExpensesUseCase.execute());
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(category).stream().map(TransactionResponse::from).toList();
    }

    /**
     * Fluxo principal por voz: recebe um áudio, transcreve, executa o comando com IA e devolve a resposta em MP3.
     */
    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")
    public ResponseEntity<Resource> processVoiceCommand(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de áudio no campo 'file'.");
        }

        var userMessage = budgetAssistant.transcribe(file.getResource());
        var answer = budgetAssistant.answer(userMessage);
        var audio = new ByteArrayResource(budgetAssistant.speak(answer));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.mp3")
                                .build()
                                .toString())
                .body(audio);
    }

    /**
     * Novo endpoint: mesmo fluxo de IA, mas recebendo e devolvendo texto.
     * Facilita testar o Tool Calling sem precisar gravar áudio.
     */
    @PostMapping(value = "/ai/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssistantTextResponse processTextCommand(@RequestBody AssistantTextRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("O campo 'message' é obrigatório.");
        }
        var answer = budgetAssistant.answer(request.message());
        return new AssistantTextResponse(request.message(), answer);
    }
}
