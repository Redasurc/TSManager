package eu.redasurc.ts3botV2.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import javax.persistence.AttributeConverter


class JsonPersistenceConverter : AttributeConverter<JsonNode, String> {
    private val  objectMapper = ObjectMapper()

    override fun convertToEntityAttribute(jsonString: String): JsonNode {
        return objectMapper.readTree(jsonString)
    }

    override fun convertToDatabaseColumn(attribute: JsonNode): String {
        return objectMapper.writeValueAsString(attribute)
    }
}