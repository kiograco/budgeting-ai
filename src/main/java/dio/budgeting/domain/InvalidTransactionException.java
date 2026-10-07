package dio.budgeting.domain;

/**
 * Lançada quando uma transação viola alguma regra de negócio do domínio.
 * <p>
 * A mesma exceção é aproveitada pelos dois "clientes" dos casos de uso:
 * a API REST (que a converte em HTTP 400) e o Tool Calling (onde o Spring AI
 * devolve a mensagem ao modelo, que pode explicar o problema ao usuário).
 */
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
