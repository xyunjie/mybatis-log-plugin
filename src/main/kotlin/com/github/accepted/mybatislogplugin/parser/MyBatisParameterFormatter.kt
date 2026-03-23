package com.github.accepted.mybatislogplugin.parser

object MyBatisParameterFormatter {
    fun format(rawValue: String, rawType: String?): String {
        val trimmedValue = rawValue.trim()
        val normalizedType = rawType?.trim()?.lowercase()

        if (trimmedValue.equals("null", ignoreCase = true)) {
            return "null"
        }

        return when {
            normalizedType == null -> quoteString(trimmedValue)
            normalizedType in NUMERIC_TYPES -> trimmedValue
            normalizedType in BOOLEAN_TYPES -> trimmedValue.lowercase()
            normalizedType in STRING_LIKE_TYPES -> quoteString(trimmedValue)
            normalizedType.contains("date") || normalizedType.contains("time") -> quoteString(trimmedValue)
            else -> quoteString(trimmedValue)
        }
    }

    private fun quoteString(value: String): String = "'" + value.replace("'", "''") + "'"

    private val NUMERIC_TYPES = setOf(
        "byte", "short", "integer", "int", "long", "float", "double", "bigdecimal", "biginteger",
    )

    private val BOOLEAN_TYPES = setOf("boolean")

    private val STRING_LIKE_TYPES = setOf(
        "string", "char", "character", "uuid",
    )
}
