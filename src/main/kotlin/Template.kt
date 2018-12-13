package me.sunnydaydev.genom

import kotlinx.serialization.json.JSON
import java.io.File
import java.lang.Error
import java.text.SimpleDateFormat
import java.util.*

class TemplateResolver(
    private val appConfig: AppConfig,
    private val json: JSON,
    private val files: Files
) {

    fun availableTemplates(): List<Template> = files.templates.list().mapNotNull(::get)

    fun get(name: String): Template? {

        val templateRoot = files.template(name) ?: return null

        val templateConfig = json.parse(TemplateConfig.serializer(), File(templateRoot, "config.json").readText())

        val values = appConfig.values + templateConfig.values

        templateConfig.copy(
            values = values,
            variables = (templateConfig.variables + appConfig.variables).filter { !values.containsKey(it.name) }
        )

        return Template(templateRoot, templateConfig)

    }

}

class Template(private val rootDir: File, private val config: TemplateConfig) {

    private val contentDir = File(rootDir, "content")
    private val targetDir get() =  File(resolve(config.path))

    private val variableValues = mutableMapOf<Variable, String>()
    private var values = config.values
    private var valuesChanged = true

    val name get() = rootDir.name

    val description get() = config.description

    init {
        updateResolvedValues()
    }

    private fun updateResolvedValues() {
        values = resolveValues(
            values = values,
            variables = variableValues.mapKeys { (variable, _) -> variable.name }
        )
        valuesChanged = false
    }

    fun files(): Sequence<TemplateFile> = contentDir.walkBottomUp()
        .filter { !it.isDirectory }
        .map {
            val targetPath = resolve(it.absolutePath.replace(contentDir.absolutePath, ""))
            TemplateFile(
                source = it,
                target = File(targetDir, targetPath)
            )
        }

    fun variables(unresolvedOnly: Boolean = true): List<Variable> =
        if (unresolvedOnly) config.variables.filterNot(variableValues::containsKey)
        else config.variables

    operator fun set(key: Variable, value: String) {
        if (config.variables.contains(key)) {
            variableValues[key] = value
            valuesChanged = true
        }
    }

    fun resolve(content: String): String {

        if (valuesChanged) {
            updateResolvedValues()
        }

        return resolve(content, values)

    }

    private fun resolveValues(values: Map<String, String>, variables: Map<String, String>): Map<String, String> {

        val allNames = (variables.map { it.key } + values.map { it.key }).toSet()

        @Suppress("unused")
        fun String.isResolved(): Boolean {

            val regex = Regex("\\\$\\{([^}]*?)}")
            val match = regex.findAll(this).toList().flatMap { it.groupValues.drop(1) }

            return if (match.isNotEmpty()) {
                !match
                    .map { it.split("|")[0] }
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

    private fun resolve(content: String, values: Map<String, String>): String {
        var result = content

        val regex = Regex("\\\$\\{([^}]*?)}")
        val match = regex.findAll(content).toList().flatMap { it.groupValues.drop(1) }

        if (match.isNotEmpty()) {

            match.forEach {

                val splitted = it.split("|")
                val name = splitted[0]

                val predefined = PREDEFINED_WORDS.contains(name.toLowerCase())
                val configurable = values.containsKey(name)

                if (!(predefined || configurable)) return@forEach

                val modifiers: List<String> = splitted.drop(1)

                val resolvedValue: String = when {
                    configurable -> resolveConfigurable(values[name]!!, modifiers)
                    predefined -> resolvePredefined(name, modifiers)
                    else -> return@forEach
                }

                result = result.replace("\${$it}", resolvedValue)

            }

        }

        return result

    }

    private fun resolvePredefined(name: String,
                                  modifiers: List<String>): String = when(name.toLowerCase()) {
        "date" -> resolveDate(modifiers)
        else -> error("Unknown predefined word.")
    }

    companion object {

        private val PREDEFINED_WORDS = setOf("date")

        private fun resolveDate(modifiers: List<String>): String {

            val date = Date()

            val format = modifiers.find { it.startsWith("format=") }
                ?.let { it.split("=")[1] }
                ?: "dd.MM.yyyy"

            return SimpleDateFormat(format, Locale.getDefault()).format(date)

        }

        private fun resolveConfigurable(value: String, modifiers: List<String>): String {

            var resultString = value

            modifiers.forEach {
                when(it) {
                    "spacelineDivider" -> resultString = resultString.toWords()
                        .let { words ->
                            if (words.isEmpty()) ""
                            else words.joinToString(separator = "_")
                        }

                    "lowercase" -> resultString = resultString.toLowerCase()
                    "uppercase" -> resultString = resultString.toUpperCase()
                    "capitalize" -> resultString = resultString.capitalize()
                    "decapitalize" -> resultString = resultString.decapitalize()
                }
            }

            return resultString

        }

        private fun String.toWords(): List<String> {

            val words = mutableListOf<String>()

            val currentWord = mutableListOf<Char>()

            forEach {
                when {
                    it.isUpperCase() -> {
                        val currentLast = lastOrNull()
                        if (currentLast != null && currentLast.isLowerCase()) {
                            if (currentWord.isNotEmpty()) {
                                words.add(currentWord.joinToString(separator = ""))
                            }
                            currentWord.clear()
                        }
                        currentWord.add(it)
                    }
                    else -> currentWord.add(it)
                }
            }

            if (currentWord.isNotEmpty()) {
                words.add(currentWord.joinToString(separator = ""))
            }

            return words

        }

    }

    data class TemplateFile(val source: File, val target: File)

}