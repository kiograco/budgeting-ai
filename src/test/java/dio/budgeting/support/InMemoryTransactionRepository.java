package dio.budgeting.support;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.Transaction;
import dio.budgeting.domain.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação em memória do repositório, usada para testar os casos de uso
 * sem banco de dados e sem chave da OpenAI.
 */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findAllByCategory(Category category) {
        return transactions.stream().filter(t -> t.getCategory() == category).toList();
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions);
    }
}
