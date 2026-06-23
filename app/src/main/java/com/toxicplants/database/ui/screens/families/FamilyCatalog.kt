package com.toxicplants.database.ui.screens.families

import kotlinx.serialization.Serializable

enum class FamilyToxicityScope(val label: String) {
    ALL_GENERA("Todos los géneros tóxicos"),
    SOME_GENERA("Algunos géneros tóxicos"),
    SOME_SPECIES("Algunas especies tóxicas"),
    UNKNOWN("Desconocido")
}

@Serializable
data class ToxicFamily(
    val family: String,
    val commonNameEs: String = "",
    val generaCount: Int = 0,
    val speciesCount: Int = 0,
    val distribution: String = "",
    val description: String = "",
    val toxicityScope: FamilyToxicityScope = FamilyToxicityScope.SOME_SPECIES,
    val notes: String = ""
)

object FamilyCatalog {
    val all = listOf(
        ToxicFamily(
            "Acanthaceae",
            "Acantáceas",
            256,
            2770,
            "Trópicos y subtrópicos",
            "Las Acanthaceae son una familia botánica que reúne 256 géneros y unas 2.770 especies de zonas tropicales y subtropicales.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Amaryllidaceae",
            "Amarilidáceas",
            75,
            1600,
            "Cosmopolita",
            "Bulbosas, alcaloides tipo licorina/galantamina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Anacardiaceae",
            "Anacardiáceas",
            83,
            860,
            "Trópicos",
            "Resinas con urushioles, dermatitis de contacto.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Apiaceae",
            "Apiáceas",
            434,
            3780,
            "Cosmopolita",
            "Umbelíferas, cicutoxina, coniina, oenantotoxina. Muy confundibles.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Apocynaceae",
            "Apocináceas",
            366,
            5100,
            "Trópicos",
            "Látex con cardenólidos, oleandrina, thevetina. Mortal.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Aquifoliaceae",
            "Aquifoliáceas",
            1,
            585,
            "Cosmopolita",
            "Acebos, ilicina.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Araceae",
            "Aráceas",
            140,
            3750,
            "Trópicos",
            "Cristales de oxalato cálcico, irritación intensa.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Araliaceae",
            "Araliáceas",
            46,
            1500,
            "Trópicos",
            "Saponinas, hederagenina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Aristolochiaceae",
            "Aristoloquiáceas",
            7,
            600,
            "Trópicos",
            "Ácido aristolóquico, nefrotóxico y carcinógeno.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Asparagaceae",
            "Asparagáceas",
            114,
            2900,
            "Cosmopolita",
            "Saponinas, convallatoxina en Convallaria.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Asteraceae",
            "Asteráceas",
            1700,
            25000,
            "Cosmopolita",
            "Pirrolizidínicos en Senecio, lactonas alergénicas.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Berberidaceae",
            "Berberidáceas",
            18,
            700,
            "Templado N",
            "Berberina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Boraginaceae",
            "Boragináceas",
            148,
            2700,
            "Cosmopolita",
            "Alcaloides pirrolizidínicos, hepatotóxicos.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Brassicaceae",
            "Brasicáceas",
            338,
            3700,
            "Cosmopolita",
            "Glucosinolatos.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Buxaceae",
            "Buxáceas",
            6,
            120,
            "Templado",
            "Ciclobuxina.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Cactaceae",
            "Cactáceas",
            127,
            1750,
            "América",
            "Alcaloides fenil-etilamínicos en algunos géneros.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Campanulaceae",
            "Campanuláceas",
            84,
            2380,
            "Cosmopolita",
            "Látex irritante en algunos.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Cannabaceae",
            "Canabáceas",
            11,
            170,
            "Norte templado",
            "THC/CBD en Cannabis.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Caprifoliaceae",
            "Caprifoliáceas",
            42,
            890,
            "Norte templado",
            "Glucósidos cianogénicos en Sambucus verde.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Caryophyllaceae",
            "Cariofiláceas",
            81,
            2200,
            "Cosmopolita",
            "Saponinas.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Celastraceae",
            "Celastráceas",
            98,
            1300,
            "Trópicos",
            "Evonina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Colchicaceae",
            "Colquicáceas",
            15,
            285,
            "Eurasia/África",
            "Colchicina. Mortal. Toda la familia.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Convolvulaceae",
            "Convolvuláceas",
            60,
            1900,
            "Trópicos",
            "Alcaloides ergolínicos en Ipomoea.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Coriariaceae",
            "Coriaríaceas",
            1,
            15,
            "Disyunta",
            "Coriamirtina, convulsivante.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Cucurbitaceae",
            "Cucurbitáceas",
            95,
            965,
            "Trópicos",
            "Cucurbitacinas amargas.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Cycadaceae",
            "Cicadáceas",
            1,
            120,
            "Trópicos",
            "Cicasina, neurotóxica y hepatotóxica.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Ericaceae",
            "Ericáceas",
            126,
            4250,
            "Cosmopolita",
            "Grayanotoxinas, miel loca.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Euphorbiaceae",
            "Euforbiáceas",
            218,
            6745,
            "Trópicos",
            "Látex con ésteres de forbol, ricina. Muy tóxica.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Fabaceae",
            "Fabáceas",
            765,
            19500,
            "Cosmopolita",
            "Alcaloides quinolizidínicos, lectinas, abrina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Gelsemiaceae",
            "Gelsemiáceas",
            2,
            11,
            "Asia/América",
            "Gelsemina, mortal.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Iridaceae",
            "Iridáceas",
            66,
            2244,
            "Cosmopolita",
            "Irritantes gastrointestinales.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Lamiaceae",
            "Lamiáceas",
            236,
            7000,
            "Cosmopolita",
            "Aceites esenciales a dosis altas.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Liliaceae",
            "Liliáceas",
            15,
            610,
            "Norte templado",
            "Nefrotóxico en gatos.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Loganiaceae",
            "Loganiáceas",
            13,
            420,
            "Trópicos",
            "Estricnina, brucina.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Malvaceae",
            "Malváceas",
            244,
            4225,
            "Cosmopolita",
            "Generalmente baja toxicidad.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Melanthiaceae",
            "Melantiáceas",
            16,
            170,
            "Norte templado",
            "Veratrina, zigadenina. Muy tóxica.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Meliaceae",
            "Meliáceas",
            50,
            600,
            "Trópicos",
            "Meliatoxinas.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Menispermaceae",
            "Menispermáceas",
            70,
            420,
            "Trópicos",
            "Tubocurarina, alcaloides bisbencílicos.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Oleaceae",
            "Oleáceas",
            25,
            600,
            "Cosmopolita",
            "Ligustrina en Ligustrum.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Papaveraceae",
            "Papaveráceas",
            44,
            770,
            "Norte templado",
            "Alcaloides isoquinolínicos, morfina.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Phytolaccaceae",
            "Fitolacáceas",
            5,
            65,
            "América",
            "Fitolacatoxina.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Plantaginaceae",
            "Plantagináceas",
            90,
            1900,
            "Cosmopolita",
            "Digitálicos en Digitalis.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Poaceae",
            "Poáceas",
            771,
            12000,
            "Cosmopolita",
            "Temulina en Lolium con endófitos.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Polygonaceae",
            "Poligonáceas",
            48,
            1200,
            "Cosmopolita",
            "Oxalatos.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Ranunculaceae",
            "Ranunculáceas",
            43,
            2000,
            "Cosmopolita",
            "Protoanemonina, aconitina. Prácticamente toda tóxica.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Rhamnaceae",
            "Ramnaceas",
            52,
            900,
            "Cosmopolita",
            "Antracenósidos.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Rosaceae",
            "Rosáceas",
            90,
            3000,
            "Cosmopolita",
            "Glucósidos cianogénicos en semillas.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Rubiaceae",
            "Rubiáceas",
            611,
            13500,
            "Trópicos",
            "Algunas tóxicas.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Rutaceae",
            "Rutáceas",
            150,
            2000,
            "Trópicos",
            "Furocumarinas fototóxicas.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Santalaceae",
            "Santaláceas",
            43,
            1000,
            "Cosmopolita",
            "Viscotoxinas en Viscum.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Sapindaceae",
            "Sapindáceas",
            135,
            1580,
            "Trópicos",
            "Esculina en Aesculus.",
            FamilyToxicityScope.SOME_GENERA
        ),
        ToxicFamily(
            "Scrophulariaceae",
            "Escrofulariáceas",
            65,
            1800,
            "Cosmopolita",
            "Glucósidos iridoides.",
            FamilyToxicityScope.SOME_SPECIES
        ),
        ToxicFamily(
            "Solanaceae",
            "Solanáceas",
            95,
            2400,
            "Cosmopolita",
            "Solanina, tropanos, nicotina. Muy tóxica.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Taxaceae",
            "Taxáceas",
            6,
            30,
            "Norte templado",
            "Taxina. Mortal. Toda la familia.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Thymelaeaceae",
            "Timeleáceas",
            50,
            900,
            "Cosmopolita",
            "Mezereína, dafnetoxina.",
            FamilyToxicityScope.ALL_GENERA
        ),
        ToxicFamily(
            "Verbenaceae",
            "Verbenáceas",
            32,
            800,
            "Trópicos",
            "Lantadenos en Lantana.",
            FamilyToxicityScope.SOME_GENERA
        ),
    ).sortedBy { it.family }
}