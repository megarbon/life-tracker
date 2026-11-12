// Tablas de menús de demo: los gramos son datos de ejemplo, no lógica, así que no tiene sentido nombrarlos.
@file:Suppress("MagicNumber")

package com.example.lifet.core.demo.nutrition

/** Una ración de un alimento de la semilla (por id) en gramos. */
internal data class DemoPortion(
    val foodId: String,
    val grams: Int,
)

private fun menu(vararg portions: Pair<String, Int>): List<DemoPortion> =
    portions.map { (id, grams) -> DemoPortion("seed_$id", grams) }

/**
 * Menús creíbles de una persona en España: desayunos repetitivos entre semana, comidas variadas y fines de semana
 * con más cerveza, raciones y postre. Todos los ids existen en `foods_es.csv` (lo comprueba un test).
 */
internal object NutritionDemoMenus {
    val usualBreakfast = menu("cafe_leche" to 200, "tostada_tomate" to 80)
    val oatBreakfast = menu("avena" to 40, "leche_semi" to 200, "platano" to 120)
    val yogurtBreakfast = menu("yogur_natural" to 125, "muesli" to 40, "cafe_solo" to 50)

    val weekendBreakfasts =
        listOf(
            menu("croissant" to 60, "zumo_naranja" to 200, "cafe_leche" to 200),
            menu("huevo_frito" to 60, "bacon" to 30, "pan_blanco" to 40, "cafe_leche" to 200),
            menu("tostada_tomate" to 120, "jamon_serrano" to 30, "zumo_naranja" to 200, "cafe_leche" to 200),
        )

    val lunches =
        listOf(
            menu("lentejas_chorizo" to 300, "pan_blanco" to 40, "manzana" to 180),
            menu("paella" to 300, "ensalada_mixta" to 150, "aceite_oliva" to 10),
            menu("arroz_pollo" to 300, "naranja" to 150),
            menu("macarrones_tomate" to 250, "yogur_natural" to 125),
            menu("pechuga_pollo" to 150, "arroz_blanco" to 180, "ensalada_mixta" to 150, "aceite_oliva" to 10),
            menu("merluza" to 150, "patata" to 200, "judias_verdes" to 150, "aceite_oliva" to 10),
            menu("cocido" to 400, "pan_blanco" to 40),
            menu("espaguetis_bolonesa" to 300, "ensalada_mixta" to 100, "pera" to 170),
            menu("garbanzos" to 200, "espinacas" to 100, "huevo" to 60, "aceite_oliva" to 10),
            menu("salmon" to 150, "quinoa" to 150, "brocoli" to 150),
            menu("lasana" to 300, "ensalada_mixta" to 100),
            menu("gazpacho" to 250, "tortilla_patatas" to 150, "pan_blanco" to 30),
            menu("fabada" to 350, "pan_blanco" to 40),
            menu("pisto" to 200, "huevo_frito" to 60, "pan_blanco" to 40),
            menu("bocadillo_jamon" to 150, "manzana" to 180),
        )

    val weekendLunches =
        listOf(
            menu("paella" to 400, "cerveza" to 330, "flan" to 100),
            menu("calamares_romana" to 150, "patatas_bravas" to 150, "croquetas" to 100, "cerveza" to 660),
            menu("cocido" to 450, "pan_blanco" to 60, "vino_tinto" to 150, "helado" to 80),
            menu("hamburguesa" to 250, "patatas_bravas" to 100, "cola" to 330),
            menu("ternera" to 200, "patata" to 200, "vino_tinto" to 150, "flan" to 100),
        )

    val snacks =
        listOf(
            menu("yogur_natural" to 125, "fresas" to 100),
            menu("almendras" to 25),
            menu("galletas_maria" to 30, "cafe_leche" to 200),
            menu("manzana" to 180),
            menu("pan_integral" to 40, "pavo_fiambre" to 30),
            menu("chocolate_negro" to 20, "te" to 200),
        )

    val weekendSnacks =
        listOf(
            menu("patatas_bolsa" to 50, "cerveza" to 330),
            menu("helado" to 100),
            menu("palomitas" to 40, "cola" to 330),
            menu("donut" to 60, "cafe_leche" to 200),
        )

    val dinners =
        listOf(
            menu("tortilla_francesa" to 100, "ensalada_mixta" to 150, "pan_integral" to 30),
            menu("crema_verduras" to 300, "queso_fresco" to 50, "yogur_desnatado" to 125),
            menu("dorada" to 150, "calabacin" to 150, "aceite_oliva" to 10),
            menu("sopa_pollo" to 300, "huevo" to 60, "pan_blanco" to 30),
            menu("sandwich_mixto" to 140, "tomate" to 120),
            menu("pechuga_pollo" to 150, "pimiento_rojo" to 100, "arroz_integral" to 120),
            menu("wrap" to 60, "pechuga_pollo" to 100, "lechuga" to 40, "tomate" to 60),
            menu("huevo" to 120, "champinones" to 100, "pan_integral" to 30),
            menu("merluza" to 150, "ensalada_mixta" to 150),
        )

    val weekendDinners =
        listOf(
            menu("pizza" to 300, "cola" to 330),
            menu("hamburguesa" to 250, "cerveza" to 330),
            menu("jamon_serrano" to 60, "queso_manchego" to 50, "pan_blanco" to 60, "vino_tinto" to 300),
            menu("croquetas" to 150, "ensaladilla" to 150, "cerveza" to 330),
        )

    /** Alimentos que el usuario de demo marca como favoritos. */
    val favoriteIds = listOf("seed_cafe_leche", "seed_tostada_tomate", "seed_yogur_natural", "seed_almendras")

    val all: List<List<DemoPortion>>
        get() =
            listOf(usualBreakfast, oatBreakfast, yogurtBreakfast) + weekendBreakfasts + lunches + weekendLunches +
                snacks + weekendSnacks + dinners + weekendDinners
}
