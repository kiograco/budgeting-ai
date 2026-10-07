package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import dio.budgeting.infrastructure.ai.BudgetAssistant;
import dio.budgeting.infrastructure.http.error.ApiExceptionHandler;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os endpoints REST com MockMvc em modo standalone: sem subir o contexto completo,
 * sem banco e sem OpenAI (o assistente de IA é substituído por um mock).
 */
class TransactionControllerTest {
    private MockMvc mockMvc;
    private BudgetAssistant budgetAssistant;

    @BeforeEach
    void setUp() {
        var repository = new InMemoryTransactionRepository();
        budgetAssistant = mock(BudgetAssistant.class);

        var controller = new TransactionController(
                new PersistTransactionUseCase(repository),
                new ListTransactionsByCategoryUseCase(repository),
                new SummarizeExpensesUseCase(repository),
                budgetAssistant);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void should_createTransaction_and_returnValueInReais() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Compras no mercado", "category": "GROCERIES", "amount": 8000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Compras no mercado"))
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.amount").value(80.0));
    }

    @Test
    void should_return400_when_amountIsNotPositive() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Mercado", "category": "GROCERIES", "amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transação inválida"));
    }

    @Test
    void should_return400_when_categoryInPathIsUnknown() throws Exception {
        mockMvc.perform(get("/transactions/PADARIA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    void should_returnSummary() throws Exception {
        for (var body : new String[]{
                "{\"description\": \"Mercado\", \"category\": \"GROCERIES\", \"amount\": 8000}",
                "{\"description\": \"Remédio\", \"category\": \"PHARMA\", \"amount\": 4000}"}) {
            mockMvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(120.0))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.categories[0].category").value("GROCERIES"))
                .andExpect(jsonPath("$.categories[0].total").value(80.0));
    }

    @Test
    void should_answerTextCommand_usingTheAssistant() throws Exception {
        when(budgetAssistant.answer("Gastei 80 reais no mercado"))
                .thenReturn("Pronto, registrei um gasto de 80 reais no mercado.");

        mockMvc.perform(post("/transactions/ai/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Gastei 80 reais no mercado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Gastei 80 reais no mercado"))
                .andExpect(jsonPath("$.answer").value("Pronto, registrei um gasto de 80 reais no mercado."));
    }

    @Test
    void should_return400_when_textCommandIsBlank() throws Exception {
        mockMvc.perform(post("/transactions/ai/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "   "}
                                """))
                .andExpect(status().isBadRequest());

        verify(budgetAssistant, never()).answer(anyString());
    }
}
