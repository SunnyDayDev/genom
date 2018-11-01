package me.sunnydaydev.genom

import kotlinx.serialization.json.JSON
import kotlin.system.exitProcess

/**
 * Created by sunny on 27/10/2018.
 * mail: mail@sunnydaydev.me
 */

fun main(args: Array<String>) {

    val json = JSON(strictMode = false)

    val appConfig: AppConfig = json.parse(AppConfig.serializer(), Files.rootConfig.readText())
    val files = Files(appConfig)
    val templateResolver = TemplateResolver(appConfig, json, files)

    val argsResolver = ArgsResolver(args)

    App(argsResolver, templateResolver).run()

}

class App(
    private val argsResolver: ArgsResolver,
    private val templateResolver: TemplateResolver
) {

    fun run() {

        val command = argsResolver.command()
        when(command) {
            null -> {
                val template = requestTemplateByName()
                handleTemplate(template)
            }
            is ArgsCommand.Template -> {

                val template = templateResolver.get(command.name) ?: {
                    println("Template '${command.name}' not exists.")
                    requestTemplateByName()
                }()

                handleTemplate(template)

            }
        }

    }

    private fun requestTemplateByName(): Template {

        val templates = templateResolver.availableTemplates()

        templates
            .mapIndexed { i, template -> "${i + 1}) ${template.name} - ${template.description}" }
            .forEach { println(it) }

        val templateName = requestInput("Choose template").let {

            if (it.all { c -> c.isDigit() }) {

                val index = it.toInt() - 1

                if (index < 0 || index > templates.lastIndex) {
                    println("Wrong index, should be in range 1..${templates.size}")
                    exitProcess(1)
                } else {
                    templates[index].name
                }

            } else {
                it
            }

        }

        return templateResolver.get(templateName) ?: {
            println("Wrong template name should be one of: ${templates.joinToString { it.name }}")
            exitProcess(1)
        }()

    }

    private fun handleTemplate(template: Template) {
        prepareTemplate(template)
        proceedTemplate(template)
    }

    private fun prepareTemplate(template: Template)  {

        template.variables().forEach {

            template[it] = it.consoleKeys.mapNotNull(argsResolver::option).firstOrNull()
                    ?: requestInput(it.description)

        }

    }

    private fun proceedTemplate(template: Template) {

        template.files().forEach {

            val content = it.source.readText()
            val resolvedContent = template.resolve(content)

            if (!it.target.parentFile.exists()) {
                it.target.parentFile.mkdirs()
            }

            it.target.writeText(resolvedContent)

        }

    }

    private fun requestInput(description: String): String {
        print("$description: ")
        return readLine()!!
    }

}