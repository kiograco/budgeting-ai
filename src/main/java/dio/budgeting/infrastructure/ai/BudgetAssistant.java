package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeExpensesUseCase;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Orquestra o fluxo do assistente de orçamento:
 * áudio → texto (transcrição) → IA com Tool Calling (casos de uso) → texto → áudio.
 * <p>
 * No projeto base essa orquestração ficava dentro do controller. Extraí para
 * esta classe para o controller cuidar apenas de HTTP e para que o mesmo fluxo
 * possa ser usado pelo endpoint de áudio e pelo novo endpoint de texto.
 */
@Component
public class BudgetAssistant {
    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;
    private final ChatClient chatClient;

    public BudgetAssistant(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel,
                           ChatClient.Builder chatClientBuilder,
                           @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                           PersistTransactionUseCase persistTransactionUseCase,
                           ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                           SummarizeExpensesUseCase summarizeExpensesUseCase) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(persistTransactionUseCase, listTransactionsByCategoryUseCase, summarizeExpensesUseCase)
                .build();
    }

    /**
     * Converte o áudio enviado pelo cliente em texto.
     */
    public String transcribe(Resource audio) {
        return transcriptionModel.transcribe(audio);
    }

    /**
     * Envia o comando em texto para a IA, que decide qual ferramenta (caso de uso) executar
     * e devolve a resposta final em texto.
     */
    public String answer(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Sintetiza a resposta em áudio MP3.
     */
    public byte[] speak(String text) {
        return textToSpeechModel.call(text);
    }
}
