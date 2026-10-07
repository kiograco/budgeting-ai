package dio.budgeting.application;

import dio.budgeting.application.output.ExpenseSummaryOutput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Novo caso de uso (evolução do projeto): resume os gastos por categoria.
 * <p>
 * É exposto tanto como ferramenta para a IA ("quanto eu gastei no total?",
 * "em qual categoria eu mais gastei?") quanto pelo endpoint REST
 * {@code GET /transactions/summary}.
 */
@Service
public class SummarizeExpensesUseCase {
    private final TransactionRepository transactionRepository;

    public SummarizeExpensesUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "summarize-expenses",
            description = "Resume os gastos registrados: valor total em reais, quantidade de transações "
                    + "e total por categoria, ordenado da categoria com maior gasto para a menor. "
                    + "Use para perguntas como 'quanto gastei no total?' ou 'onde eu mais gastei?'.")
    public ExpenseSummaryOutput execute() {
        List<Transaction> transactions = transactionRepository.findAll();

        Map<Category, long[]> totals = new EnumMap<>(Category.class); // [0] = centavos, [1] = quantidade
        for (Transaction transaction : transactions) {
            long[] accumulator = totals.computeIfAbsent(transaction.getCategory(), c -> new long[2]);
            accumulator[0] += transaction.getAmount();
            accumulator[1]++;
        }

        List<CategoryTotal> categories = totals.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Category, long[]> e) -> e.getValue()[0]).reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(e -> new CategoryTotal(
                        e.getKey().name(),
                        TransactionOutput.centsToReais(e.getValue()[0]),
                        e.getValue()[1]))
                .toList();

        long totalCents = transactions.stream().mapToLong(Transaction::getAmount).sum();

        return new ExpenseSummaryOutput(
                TransactionOutput.centsToReais(totalCents),
                transactions.size(),
                categories);
    }
}
