package me.sunnydaydev.genom

import kotlinx.serialization.json.JSON
import java.io.File

class TemplateResolver(
    private val appConfig: AppConfig,
    private val json: JSON,
    private val files: Files
) {

    fun availableTemplates(): List<Template> = files.templates.list().mapNotNull(::get)

    fun get(name: String): Template? {

        val templateRoot = files.template(name) ?: return null

        val templateConfig = json.parse(TemplateConfig.serializer(), File(templateRoot, "config.json").readText())
        templateConfig.copy(
            values = templateConfig.values + appConfig.values,
            variables = templateConfig.variables + appConfig.variables
        )

        return Template(templateRoot, templateConfig)

    }

}

class Template(private val rootDir: File, private val config: TemplateConfig) {

    private val contentDir = File(rootDir, "content")

    val name get() = rootDir.name

    val description get() = config.description

    fun files(): List<File> = contentDir.walkBottomUp().filter { it.isDirectory } .toList()

}