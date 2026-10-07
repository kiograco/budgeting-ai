package dio.budgeting.application;

import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.output.ExpenseSummaryOutput.CategoryTotal;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.InvalidTransactionException;
import dio.budgeting.support.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa os casos de uso que também são expostos como ferramentas (Tool Calling) para a IA.
 * Não precisa de banco de dados nem de chave da OpenAI.
 */
class TransactionUseCasesTest {
    private InMemoryTransactionRepository repository;
    private PersistTransactionUseCase persist;
    private ListTransactionsByCategoryUseCase listByCategory;
    private SummarizeExpensesUseCase summarize;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        persist = new PersistTransactionUseCase(repository);
        listByCategory = new ListTransactionsByCategoryUseCase(repository);
        summarize = new SummarizeExpensesUseCase(repository);
    }

    @Test
    void should_persistTransaction_and_returnValueInReais() {
        var output = persist.execute(new PersistTransactionInput("Compras no mercado", 8050, Category.GROCERIES));

        assertThat(output.id()).isNotBlank();
        assertThat(output.description()).isEqualTo("Compras no mercado");
        assertThat(output.category()).isEqualTo("GROCERIES");
        assertThat(output.value()).isEqualTo(80.50);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void should_notPersist_when_inputIsInvalid() {
        assertThatThrownBy(() -> persist.execute(new PersistTransactionInput("Mercado", 0, Category.GROCERIES)))
                .isInstanceOf(InvalidTransactionException.class);

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void should_listOnlyTransactionsOfTheRequestedCategory() {
        persist.execute(new PersistTransactionInput("Mercado", 8000, Category.GROCERIES));
        persist.execute(new PersistTransactionInput("Remédio", 4000, Category.PHARMA));
        persist.execute(new PersistTransactionInput("Feira", 2000, Category.GROCERIES));

        var groceries = listByCategory.execute(Category.GROCERIES);

        assertThat(groceries).extracting(TransactionOutput::description).containsExactly("Mercado", "Feira");
        assertThat(groceries).extracting(TransactionOutput::value).containsExactly(80.0, 20.0);
    }

    @Test
    void should_summarizeExpenses_byCategory_orderedByHighestTotal() {
        persist.execute(new PersistTransactionInput("Mercado", 8000, Category.GROCERIES));
        persist.execute(new PersistTransactionInput("Feira", 4000, Category.GROCERIES));
        persist.execute(new PersistTransactionInput("Remédio", 4000, Category.PHARMA));
        persist.execute(new PersistTransactionInput("Combustível", 20000, Category.AUTO));

        var summary = summarize.execute();

        assertThat(summary.total()).isEqualTo(360.0);
        assertThat(summary.transactionCount()).isEqualTo(4);
        assertThat(summary.categories())
                .extracting(CategoryTotal::category, CategoryTotal::total, CategoryTotal::transactionCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("AUTO", 200.0, 1L),
                        org.assertj.core.groups.Tuple.tuple("GROCERIES", 120.0, 2L),
                        org.assertj.core.groups.Tuple.tuple("PHARMA", 40.0, 1L));
    }

    @Test
    void should_returnEmptySummary_when_thereAreNoTransactions() {
        var summary = summarize.execute();

        assertThat(summary.total()).isZero();
        assertThat(summary.transactionCount()).isZero();
        assertThat(summary.categories()).isEmpty();
    }

    @Test
    void should_convertCentsToReais() {
        assertThat(TransactionOutput.centsToReais(8000)).isEqualTo(80.0);
        assertThat(TransactionOutput.centsToReais(1250)).isEqualTo(12.5);
        assertThat(TransactionOutput.centsToReais(1)).isEqualTo(0.01);
    }
}
