package me.sunnydaydev.genom

import kotlinx.serialization.json.JSON
import java.lang.Error

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

    private var nextAction: Action = handleArgs()

    fun run() {

        while (nextAction.canExecute) {
            nextAction.execute()
        }

        /*
        if (config.variables.isNotEmpty()) {

            config.variables.forEach {

                val consoleValue = consoleKeyInput[it.consoleKey]

                if (consoleValue != null) {

                    variableValues[it.name] = consoleValue

                } else {

                    print("${it.description}: ")
                    variableValues[it.name] = readLine()!!

                }

            }

        }

        val resolvedValues = resolveValues(variableValues, config.values)

        val targetDir = File("./", resolve(config.path, resolvedValues))

        moduleContentDir.copyRecursively(targetDir, overwrite = true)

        targetDir.walkBottomUp().forEach { file ->
            val resolvedFileName = resolve(file.name, resolvedValues)
            if (resolvedFileName != file.name) {
                file.renameTo(File(file.parent, resolvedFileName))
            }
        }

        targetDir.walkBottomUp()
            .filter { !it.isDirectory }
            .forEach {
                val fileContent: String = it.readText()
                val resolvedContent = resolve(fileContent, resolvedValues)
                if (resolvedContent != fileContent) {
                    it.writeText(resolvedContent)
                }
            }

            */

    }

    private fun handleArgs() = Action.create {
        val command = argsResolver.command()
        nextAction = when(command) {
            null -> requestTemplateName()
            is ArgsCommand.Module -> checkTemplate(command.name)
        }
    }

    private fun requestTemplateName() = Action.create {

        val templates = templateResolver.availableTemplates()

        templates
            .mapIndexed { i, template -> "${i + 1}) ${template.name} - ${template.description}" }
            .forEach { println(it) }

        var templateName = readLine()!!.trim()

        if (templateName.all { c -> c.isDigit() }) {

            val index = templateName.toInt()

            if (index < 1 || index > templates.size) {
                println("Wrong index, should be in range 1..${templates.size}")
                return@create
            } else {
                templateName = templates[index].name
            }

        }

        val template = templateResolver.get(templateName)

        if (template ==  null) {
            println("Wrong template name should be one of: ${templates.joinToString { it.name }}")
            return@create
        } else {
            proceedTemplate(template)
        }

    }

    private fun checkTemplate(name: String) = Action.create {

        val template = templateResolver.get(name)

        nextAction =
                if (template == null) requestTemplateName()
                else proceedTemplate(template)

    }

    private fun proceedTemplate(template: Template) = Action.create {

    }

    class Action(private val run: () -> Unit) {

        var canExecute = true
            private set

        fun execute() {
            canExecute = false
            run()
        }

        companion object {

            fun create(run: () -> Unit) = Action(run)

        }

    }

}

fun resolveValues(variables: Map<String, String>, values: Map<String, String>): Map<String, String> {

    val allNames = (variables.map { it.key } + values.map { it.key }).toSet()

    @Suppress("unused")
    fun String.isResolved(): Boolean {

        val regex = Regex("\\\$\\{([^}]*?)}")
        val match = regex.findAll(this).toList().flatMap { it.groupValues.drop(1) }

        return if (match.isNotEmpty()) {
            !match
                .map { it.split(":")[0] }
                .any { allNames.contains(it) }
        } else {
            true
        }

    }

    val resolvedValues: MutableMap<String, String> = (variables + values.filter { (_, value) -> value.isResolved() })
        .toMutableMap()

    val unresolvedValues: MutableMap<String, String> = (values.filterKeys { !resolvedValues.containsKey(it) })
        .toMutableMap()

    while(unresolvedValues.isNotEmpty()) {

        val resolutionStepValues = unresolvedValues
            .mapNotNull { (key, value) ->
                val resolvedValue = resolve(value, resolvedValues)
                if (resolvedValue != value) key to resolvedValue else null
            }

        if (resolutionStepValues.isEmpty()) {
            throw Error("Unresolvable step.")
        }

        resolvedValues.putAll(resolutionStepValues)
        resolutionStepValues.forEach { (key, _) -> unresolvedValues.remove(key) }

    }

    return resolvedValues.toMap()

}

fun resolve(source: String, values: Map<String, String>): String {
    var result = source

    val regex = Regex("\\\$\\{([^}]*?)}")
    val match = regex.findAll(source).toList().flatMap { it.groupValues.drop(1) }

    if (match.isNotEmpty()) {

        match.mapNotNull {

                val splitted = it.split(":")
                val name = splitted[0]

                if (!values.containsKey(name)) return@mapNotNull null

                val modifiers: List<String> = splitted.getOrNull(1)?.split(",") ?: emptyList()
                Triple(it, name, modifiers)

            }
            .forEach { (raw, name, modifiers) ->
                result = result.replace("\${$raw}", resolveValue(values[name]!!, modifiers))
            }

    }

    return result

}

fun resolveValue(value: String, modifiers: List<String>): String {

    var resultString = value

    modifiers.forEach {
        when(it) {
            "lowercase" -> resultString = resultString.toLowerCase()
            "uppercase" -> resultString = resultString.toUpperCase()
            "capitalize" -> resultString = resultString.capitalize()
            "decapitalize" -> resultString = resultString.decapitalize()
        }
    }

    return resultString

}