package org.mosyagin.project.parser

data class ParsedScene(
    val seriesNumber: String,
    val sceneNumber: String, // Изменили с Int на String для поддержки 6А, 12Б и т.д.
    val type: String,
    val location: String,
    val time: String,
    val content: String,
    val actors: List<String>
)
