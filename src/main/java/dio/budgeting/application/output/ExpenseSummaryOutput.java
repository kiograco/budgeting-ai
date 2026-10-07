package dio.budgeting.application.output;

import java.util.List;

/**
 * Resumo dos gastos: total geral e total por categoria (valores em reais).
 */
public record ExpenseSummaryOutput(double total, long transactionCount, List<CategoryTotal> categories) {

    public record CategoryTotal(String category, double total, long transactionCount) {
    }
}
