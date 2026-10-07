package dio.budgeting.application.output;

import dio.budgeting.domain.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representação de saída de uma transação.
 * <p>
 * {@code value} é o valor em reais. No domínio o valor é guardado em centavos,
 * então a conversão divide por 100 (no projeto base o valor em centavos era
 * exibido como se fosse reais: 8000 centavos apareciam como 8000.0).
 */
public record TransactionOutput(String id, String description, String category, double value) {
    public static TransactionOutput from(Transaction transaction) {
        return new TransactionOutput(
                transaction.getId().uuid().toString(),
                transaction.getDescription(),
                transaction.getCategory().name(),
                centsToReais(transaction.getAmount()));
    }

    public static double centsToReais(long cents) {
        return BigDecimal.valueOf(cents)
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
