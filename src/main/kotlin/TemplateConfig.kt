package me.sunnydaydev.genom

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Optional

/**
 * Created by sunny on 27/10/2018.
 * mail: mail@sunnydaydev.me
 */

@Serializable
data class TemplateConfig(
    @SerialName("description")
    val description: String,
    @SerialName("path")
    val path: String,
    @SerialName("values")
    val values: Map<String, String>,
    @SerialName("variables")
    val variables: List<Variable>
)

@Serializable
data class AppConfig(
    @SerialName("values")
    val values: Map<String, String>,
    @SerialName("variables")
    val variables: List<Variable>,
    @SerialName("templatesPath")
    val templatesPath: String
)

@Serializable
data class Variable(
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("description")
    val description: String,
    @SerialName("consoleKey")
    @Optional
    val consoleKeys: List<String> = emptyList()
)