package com.example.lifet.core.model.finance

/** Un movimiento pertenece a una categoría de gasto o de ingreso; la categoría decide cómo se agrega. */
public enum class CategoryKind { EXPENSE, INCOME }

/**
 * Categoría editable. Las de partida ([defaultCategory] no nulo) muestran su nombre localizado mientras
 * [customName] sea nulo; al renombrarlas se guarda el texto del usuario. [iconKey] es una clave estable que
 * la UI traduce a icono. Con movimientos asociados se archiva en lugar de borrarse.
 */
public data class FinanceCategory(
    val id: String,
    val kind: CategoryKind,
    val iconKey: String,
    val customName: String? = null,
    val defaultCategory: DefaultCategory? = null,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** Categorías de partida. Los ids son fijos para que sembrar varias veces no duplique. */
public enum class DefaultCategory(
    public val key: String,
    public val kind: CategoryKind,
    public val iconKey: String,
) {
    GROCERIES("groceries", CategoryKind.EXPENSE, "cart"),
    HOUSING("housing", CategoryKind.EXPENSE, "home"),
    SUBSCRIPTIONS("subscriptions", CategoryKind.EXPENSE, "repeat"),
    RESTAURANTS("restaurants", CategoryKind.EXPENSE, "restaurant"),
    LEISURE("leisure", CategoryKind.EXPENSE, "celebration"),
    TRANSPORT("transport", CategoryKind.EXPENSE, "commute"),
    HEALTH("health", CategoryKind.EXPENSE, "health"),
    SHOPPING("shopping", CategoryKind.EXPENSE, "bag"),
    OTHER_EXPENSE("other_expense", CategoryKind.EXPENSE, "category"),
    SALARY("salary", CategoryKind.INCOME, "work"),
    OTHER_INCOME("other_income", CategoryKind.INCOME, "savings"),
    ;

    public val id: String get() = "default_$key"
}

/** Categorías de partida, en el orden en que se muestran. */
public fun defaultFinanceCategories(): List<FinanceCategory> =
    DefaultCategory.entries.mapIndexed { index, default ->
        FinanceCategory(
            id = default.id,
            kind = default.kind,
            iconKey = default.iconKey,
            defaultCategory = default,
            sortOrder = index,
        )
    }
