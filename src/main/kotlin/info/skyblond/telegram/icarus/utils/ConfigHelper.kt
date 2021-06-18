package info.skyblond.telegram.icarus.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import info.skyblond.telegram.icarus.models.ConfigPojo
import org.slf4j.LoggerFactory
import java.io.File

object ConfigHelper {
    private val logger = LoggerFactory.getLogger(ConfigHelper::class.java)

    private val mapper = ObjectMapper(YAMLFactory())

    private val configFile = File("./config.yaml")

    var config = ConfigPojo()
        private set

    init {
        mapper.findAndRegisterModules()
        config = readConfigOrDefaultIfNotExist()
        writeConfig(config)
    }

    private fun readConfigOrDefaultIfNotExist(): ConfigPojo {
        return if (configFile.exists()) {
            logger.info("Found config at '{}'", configFile.canonicalPath)
            mapper.readValue(configFile)
        } else {
            logger.info("Config not found, init the default at '{}'", configFile.canonicalPath)
            config
        }
    }

    private fun writeConfig(config: ConfigPojo) {
        mapper.writeValue(configFile, config)
    }
}