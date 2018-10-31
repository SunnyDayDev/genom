package me.sunnydaydev.genom

class ArgsResolver(val args: Array<String>) {

    private val nonOptions = args.mapIndexedNotNull { index, s ->
            if (s.isOption) null
            else s to index
        }
        .associate { it }

    private val options = args.mapIndexedNotNull { index, s ->
            if (!s.isOption) null
            else s.dropWhile { it == '-' } to index
        }
        .associate { it }

    fun command(): ArgsCommand? {
        val nonOption = nonOptions.minBy { (_, value) -> value } ?.takeIf { it.value == 0 } ?: return null
        return ArgsCommand.Module(name = nonOption.key)
    }

    fun option(name: String): String? {
        val optionIndex = options[name] ?: return null
        return args.getOrNull(optionIndex + 1)?.takeIf { !it.isOption }
    }

    private val String.isOption get() = startsWith("-")

}

sealed class ArgsCommand {

    data class Module(val name: String): ArgsCommand()

}