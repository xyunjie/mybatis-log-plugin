package com.github.accepted.mybatislogplugin.parser

import com.github.accepted.mybatislogplugin.model.LogOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MyBatisLogParserTest {
    private val parser = MyBatisLogParser()

    @Test
    fun `parse standard preparing and parameters`() {
        val text = """
            Preparing: select * from user where id = ? and name = ?
            Parameters: 1(Integer), Alice(String)
        """.trimIndent()

        val entries = parser.parse(text, LogOrigin.MANUAL)

        assertEquals(1, entries.size)
        assertEquals("select * from user where id = 1 and name = 'Alice'", entries.single().restoredSql)
    }

    @Test
    fun `parse comma inside string parameter`() {
        val text = """
            Preparing: insert into message(content, type) values (?, ?)
            Parameters: hello,world(String), note(String)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals("insert into message(content, type) values ('hello,world', 'note')", entry.restoredSql)
    }

    @Test
    fun `parse boolean null and numeric values`() {
        val text = """
            Preparing: update user set enabled = ?, score = ?, deleted_at = ? where id = ?
            Parameters: true(Boolean), 99(Integer), null, 7(Long)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals("update user set enabled = true, score = 99, deleted_at = null where id = 7", entry.restoredSql)
    }

    @Test
    fun `skip entry when parameters line missing`() {
        val entry = parser.appendLine("Preparing: select * from user where id = ?", LogOrigin.AUTO)

        assertNull(entry)
    }

    @Test
    fun `skip entry on placeholder mismatch`() {
        val text = """
            Preparing: select * from user where id = ? and name = ?
            Parameters: 1(Integer)
        """.trimIndent()

        val entries = parser.parse(text, LogOrigin.MANUAL)

        assertEquals(0, entries.size)
    }

    @Test
    fun `parse multiple entries from one block`() {
        val text = """
            Preparing: select * from user where id = ?
            Parameters: 1(Integer)
            Preparing: select * from role where code = ?
            Parameters: admin(String)
        """.trimIndent()

        val entries = parser.parse(text, LogOrigin.MANUAL)

        assertEquals(2, entries.size)
        assertEquals("select * from user where id = 1", entries[0].restoredSql)
        assertEquals("select * from role where code = 'admin'", entries[1].restoredSql)
    }
}
