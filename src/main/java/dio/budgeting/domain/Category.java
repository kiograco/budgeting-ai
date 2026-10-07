package dio.budgeting.domain;

/**
 * Categorias de gasto aceitas pela aplicação.
 * <p>
 * RESTAURANT, LEISURE e OTHER foram adicionadas para que o assistente consiga
 * classificar comandos como "gastei 60 reais no restaurante" sem forçar uma
 * categoria errada.
 */
public enum Category {
    GROCERIES,
    PHARMA,
    AUTO,
    RESTAURANT,
    LEISURE,
    OTHER,
}
