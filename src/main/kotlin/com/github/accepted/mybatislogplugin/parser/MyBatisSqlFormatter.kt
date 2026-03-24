package com.github.accepted.mybatislogplugin.parser

object MyBatisSqlFormatter {
    fun format(sql: String): String {
        val normalized = normalizeWhitespace(sql)
        if (normalized.isBlank()) {
            return normalized.trim()
        }

        val result = StringBuilder()
        var index = 0
        var currentClause: Clause? = null
        var currentClauseDepth = 0
        var depth = 0

        while (index < normalized.length) {
            val char = normalized[index]

            if (char == '\'' || char == '"') {
                val nextIndex = appendQuotedLiteral(normalized, index, result)
                index = nextIndex
                continue
            }

            if (char == ')') {
                depth = (depth - 1).coerceAtLeast(0)
                result.append(char)
                index++
                continue
            }

            val clauseMatch = matchClause(normalized, index)
            if (clauseMatch != null) {
                if (result.isNotEmpty()) {
                    trimTrailingWhitespace(result)
                    appendLineBreak(result, depth)
                }
                result.append(normalized, index, index + clauseMatch.text.length)
                currentClause = clauseMatch.clause
                currentClauseDepth = depth
                index += clauseMatch.text.length
                continue
            }

            val logicalMatch = matchLogicalOperator(normalized, index)
            if (logicalMatch != null && currentClause in CONDITION_CLAUSES && depth >= currentClauseDepth) {
                trimTrailingWhitespace(result)
                appendLineBreak(result, currentClauseDepth + 1)
                result.append(normalized, index, index + logicalMatch.length)
                index += logicalMatch.length
                continue
            }

            when (char) {
                '(' -> {
                    result.append(char)
                    depth++
                    index++
                }
                ',' -> {
                    result.append(char)
                    index++
                    while (index < normalized.length && normalized[index] == ' ') {
                        index++
                    }
                    if (currentClause in COMMA_SPLIT_CLAUSES && depth == currentClauseDepth) {
                        appendLineBreak(result, depth + 1)
                    } else if (index < normalized.length) {
                        result.append(' ')
                    }
                }
                else -> {
                    result.append(char)
                    index++
                }
            }
        }

        return result
            .toString()
            .trim()
            .lines()
            .joinToString("\n") { it.trimEnd() }
    }

    private fun normalizeWhitespace(sql: String): String {
        val result = StringBuilder()
        var index = 0
        var pendingSpace = false

        while (index < sql.length) {
            val char = sql[index]
            when {
                char == '\'' || char == '"' -> {
                    if (pendingSpace && result.isNotEmpty() && result.last() != ' ' && result.last() != '(') {
                        result.append(' ')
                    }
                    pendingSpace = false
                    index = appendQuotedLiteral(sql, index, result)
                }
                char.isWhitespace() -> {
                    pendingSpace = result.isNotEmpty()
                    index++
                }
                else -> {
                    if (pendingSpace && result.isNotEmpty() && result.last() != ' ' && result.last() != '(') {
                        result.append(' ')
                    }
                    pendingSpace = false
                    result.append(char)
                    index++
                }
            }
        }

        return result.toString().trim()
    }

    private fun appendQuotedLiteral(source: String, startIndex: Int, target: StringBuilder): Int {
        val quote = source[startIndex]
        var index = startIndex
        var isOpeningQuote = true

        while (index < source.length) {
            val current = source[index]
            target.append(current)
            index++

            if (current == quote) {
                if (isOpeningQuote) {
                    isOpeningQuote = false
                    continue
                }

                val escapedByDoubling = source.getOrNull(index) == quote
                if (escapedByDoubling) {
                    target.append(source[index])
                    index++
                    continue
                }
                break
            }
        }

        return index
    }

    private fun matchClause(sql: String, index: Int): ClauseMatch? {
        return CLAUSE_PATTERNS.firstNotNullOfOrNull { (text, clause) ->
            if (sql.regionMatches(index, text, 0, text.length, ignoreCase = true) &&
                hasWordBoundary(sql, index - 1) &&
                hasWordBoundary(sql, index + text.length)
            ) {
                ClauseMatch(text = text, clause = clause)
            } else {
                null
            }
        }
    }

    private fun matchLogicalOperator(sql: String, index: Int): String? {
        return LOGICAL_OPERATORS.firstOrNull { operator ->
            sql.regionMatches(index, operator, 0, operator.length, ignoreCase = true) &&
                hasWordBoundary(sql, index - 1) &&
                hasWordBoundary(sql, index + operator.length)
        }
    }

    private fun hasWordBoundary(sql: String, index: Int): Boolean {
        val char = sql.getOrNull(index) ?: return true
        return !char.isLetterOrDigit() && char != '_' && char != '$'
    }

    private fun trimTrailingWhitespace(target: StringBuilder) {
        while (target.isNotEmpty() && target.last().isWhitespace()) {
            target.deleteCharAt(target.lastIndex)
        }
    }

    private fun appendLineBreak(target: StringBuilder, depth: Int) {
        target.append('\n')
        repeat(depth.coerceAtLeast(0)) {
            target.append(INDENT)
        }
    }

    private data class ClauseMatch(
        val text: String,
        val clause: Clause,
    )

    private enum class Clause {
        SELECT,
        FROM,
        WHERE,
        GROUP_BY,
        ORDER_BY,
        HAVING,
        INSERT_INTO,
        VALUES,
        UPDATE,
        SET,
        DELETE_FROM,
        JOIN,
        ON,
    }

    private const val INDENT = "    "

    private val CLAUSE_PATTERNS = listOf(
        "DELETE FROM" to Clause.DELETE_FROM,
        "INSERT INTO" to Clause.INSERT_INTO,
        "GROUP BY" to Clause.GROUP_BY,
        "ORDER BY" to Clause.ORDER_BY,
        "LEFT JOIN" to Clause.JOIN,
        "RIGHT JOIN" to Clause.JOIN,
        "INNER JOIN" to Clause.JOIN,
        "OUTER JOIN" to Clause.JOIN,
        "CROSS JOIN" to Clause.JOIN,
        "SELECT" to Clause.SELECT,
        "FROM" to Clause.FROM,
        "WHERE" to Clause.WHERE,
        "HAVING" to Clause.HAVING,
        "VALUES" to Clause.VALUES,
        "UPDATE" to Clause.UPDATE,
        "JOIN" to Clause.JOIN,
        "SET" to Clause.SET,
        "ON" to Clause.ON,
    )

    private val CONDITION_CLAUSES = setOf(Clause.WHERE, Clause.ON, Clause.HAVING)
    private val COMMA_SPLIT_CLAUSES = setOf(Clause.SELECT, Clause.SET)
    private val LOGICAL_OPERATORS = listOf("AND", "OR")
}
