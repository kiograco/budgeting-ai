package dio.budgeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto completo da aplicação. Precisa da chave da OpenAI e do MySQL
 * rodando (docker compose up -d), por isso só executa quando OPENAI_API_KEY está definida.
 * Os testes unitários (domínio, casos de uso e controller) rodam sem nenhuma dessas dependências.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class BudgetingApplicationTests {

    @Test
    void contextLoads() {
    }

}
