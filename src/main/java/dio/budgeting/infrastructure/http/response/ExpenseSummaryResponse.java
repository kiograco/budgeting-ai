package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.ExpenseSummaryOutput;

import java.util.List;

public record ExpenseSummaryResponse(double total, long transactionCount, List<CategoryTotalResponse> categories) {

    public record CategoryTotalResponse(String category, double total, long transactionCount) {
    }

    public static ExpenseSummaryResponse from(ExpenseSummaryOutput output) {
        return new ExpenseSummaryResponse(
                output.total(),
                output.transactionCount(),
                output.categories().stream()
                        .map(c -> new CategoryTotalResponse(c.category(), c.total(), c.transactionCount()))
                        .toList());
    }
}
