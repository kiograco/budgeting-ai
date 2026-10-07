package dio.budgeting.infrastructure.http.request;

/**
 * Comando em texto para o assistente, ex.: {"message": "Gastei 80 reais no mercado"}.
 */
public record AssistantTextRequest(String message) {
}
