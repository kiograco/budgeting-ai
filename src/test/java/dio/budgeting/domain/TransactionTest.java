package dio.budgeting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    void should_createTransaction_when_dataIsValid() {
        var transaction = new Transaction("  Compras no mercado  ", 8000, Category.GROCERIES);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getDescription()).isEqualTo("Compras no mercado");
        assertThat(transaction.getAmount()).isEqualTo(8000);
        assertThat(transaction.getCategory()).isEqualTo(Category.GROCERIES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_reject_when_descriptionIsBlank(String description) {
        assertThatThrownBy(() -> new Transaction(description, 8000, Category.GROCERIES))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("descrição");
    }

    @Test
    void should_reject_when_descriptionIsTooLong() {
        var description = "a".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> new Transaction(description, 8000, Category.GROCERIES))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("máximo");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -8000})
    void should_reject_when_amountIsNotPositive(long amount) {
        assertThatThrownBy(() -> new Transaction("Farmácia", amount, Category.PHARMA))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void should_reject_when_categoryIsNull() {
        assertThatThrownBy(() -> new Transaction("Farmácia", 4000, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("categoria");
    }
}
