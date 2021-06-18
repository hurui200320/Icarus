package info.skyblond.telegram.icarus.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

object JsonHelper {
    private val logger = LoggerFactory.getLogger(JsonHelper::class.java)

    fun getObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.findAndRegisterModules()
        return mapper
    }
}