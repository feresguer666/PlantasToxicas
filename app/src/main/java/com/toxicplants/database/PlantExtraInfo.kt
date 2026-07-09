package com.toxicplants.database

data class PlantExtraInfo(
    val scientificName: String,
    val toxicDogs: Boolean = false,
    val toxicCats: Boolean = false,
    val toxicHorses: Boolean = false,
    val toxicCattle: Boolean = false,
    val toxicChildren: Boolean = false,
    val flowerColor: String = "",
    val fruitColor: String = "",
    val confusableWith: List<String> = emptyList(),
    val confusionReason: String = ""
)
