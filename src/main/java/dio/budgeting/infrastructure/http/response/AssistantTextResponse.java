package dio.budgeting.infrastructure.http.response;

/**
 * Resposta do assistente em texto: o comando recebido e a resposta gerada pela IA.
 */
public record AssistantTextResponse(String message, String answer) {
}
