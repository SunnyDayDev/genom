package me.sunnydaydev.genom

import kotlinx.serialization.json.JSON
import java.io.File
import java.lang.Error

/**
 * Created by sunny on 27/10/2018.
 * mail: mail@sunnydaydev.me
 */

fun main(args: Array<String>) {

    val json = JSON(strictMode = false)

    val rootDir = File("./")
    val globalConfig: RootConfig = json.parse(RootConfig.serializer(), File(rootDir, "config.json").readText())

    val temlatesDir = File(globalConfig.templatesPath)

    val module = if (args.isEmpty()) {

        val modules = temlatesDir.listFiles()
            .filter { it.isDirectory }
            .mapNotNull {
                val configFile = File(it, "config.json")
                it to json.parse(ModuleConfig.serializer(), configFile.readText())
            }

        modules
            .mapIndexed { i, (dir, config) -> "${i + 1}) ${dir.name} - ${config.description}" }
            .forEach { println(it) }

        readLine()!!.trim().let {
            if (!it.all { c -> c.isDigit() }) it
            else modules[it.toInt() - 1].first.name
        }

    } else {
        args[0]
    }

    val moduleTemplateDir = File(temlatesDir, module)
    val moduleContentDir = File(moduleTemplateDir, "content")

    val moduleConfig = json.parse(ModuleConfig.serializer(), File(moduleTemplateDir, "config.json").readText())

    val config = moduleConfig.copy(values = moduleConfig.values + globalConfig.values)

    val variableValues = mutableMapOf<String, String>()

    val consoleKeyInput = args
        .mapIndexedNotNull { i, value ->

            if (value.startsWith("-")) {

                val key = value.substring(1)

                if (key.all { it.isLetter() }) key to args[i + 1]
                else null

            } else {
                null
            }

        }
        .associate { (key, value) -> key to value }

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