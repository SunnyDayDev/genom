package me.sunnydaydev.genom

import java.io.File

/**
 * Created by sunny on 31/10/2018.
 * mail: mail@sunnydaydev.me
 */

class Files(private val config: AppConfig) {

    companion object {

        private const val CONFIG_FILE_NAME = "config.json"
        private const val CONTENT_DIR_NAME = "content"

        val root get() = File("./")
        val rootConfig get() = File(root, CONFIG_FILE_NAME)

    }

    val templates get() = File(config.templatesPath)

    fun template(name: String): File? = File(templates, name).takeIf {
        it.exists() && it.isDirectory && File(it, CONFIG_FILE_NAME).exists() && File(it, CONTENT_DIR_NAME).exists()
    }

}