package dio.budgeting.application.input;

import dio.budgeting.domain.Category;
import org.springframework.ai.tool.annotation.ToolParam;

public record PersistTransactionInput(
        @ToolParam(description = "Descrição curta do gasto, por exemplo 'Compras no mercado'") String description,
        @ToolParam(description = "Valor do gasto em CENTAVOS (inteiro). Ex.: 80 reais = 8000; 12,50 reais = 1250")
        long amount,
        @ToolParam(description = "Categoria de uma transação") Category category) {
}
