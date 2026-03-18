package org.mosyagin.project.parser

data class ParsedScene(
    val seriesNumber: Int,
    val sceneNumber: Int,
    val type: String,
    val location: String,
    val time: String,
    val content: String,
    val actors: List<String>
)
