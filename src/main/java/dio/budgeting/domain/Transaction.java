package dio.budgeting.domain;

/**
 * Transação financeira (um gasto).
 * <p>
 * O valor ({@code amount}) é sempre armazenado em centavos para evitar
 * problemas de arredondamento com ponto flutuante.
 * <p>
 * As validações ficam no construtor: assim nenhuma transação inválida
 * chega a existir, venha ela da API REST ou de uma chamada de ferramenta da IA.
 */
public class Transaction {
    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private final TransactionId id;
    private final String description;
    private final long amount;
    private final Category category;

    public Transaction(TransactionId id, String description, long amount, Category category) {
        if (id == null) {
            throw new InvalidTransactionException("O identificador da transação é obrigatório.");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("A descrição da transação é obrigatória.");
        }
        if (description.strip().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTransactionException(
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        if (amount <= 0) {
            throw new InvalidTransactionException("O valor da transação deve ser maior que zero.");
        }
        if (category == null) {
            throw new InvalidTransactionException("A categoria da transação é obrigatória.");
        }
        this.id = id;
        this.description = description.strip();
        this.amount = amount;
        this.category = category;
    }

    public Transaction(String description, long amount, Category category) {
        this(new TransactionId(), description, amount, category);
    }

    public TransactionId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return valor em centavos
     */
    public long getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }
}
