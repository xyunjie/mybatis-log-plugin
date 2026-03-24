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
        assertEquals(
            """
            select *
            from user
            where id = 1
                and name = 'Alice'
            """.trimIndent(),
            entries.single().restoredSql,
        )
    }

    @Test
    fun `parse comma inside string parameter`() {
        val text = """
            Preparing: insert into message(content, type) values (?, ?)
            Parameters: hello,world(String), note(String)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals(
            """
            insert into message(content, type)
            values ('hello,world', 'note')
            """.trimIndent(),
            entry.restoredSql,
        )
    }

    @Test
    fun `parse boolean null and numeric values`() {
        val text = """
            Preparing: update user set enabled = ?, score = ?, deleted_at = ? where id = ?
            Parameters: true(Boolean), 99(Integer), null, 7(Long)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals(
            """
            update user
            set enabled = true,
                score = 99,
                deleted_at = null
            where id = 7
            """.trimIndent(),
            entry.restoredSql,
        )
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
        assertEquals(
            """
            select *
            from user
            where id = 1
            """.trimIndent(),
            entries[0].restoredSql,
        )
        assertEquals(
            """
            select *
            from role
            where code = 'admin'
            """.trimIndent(),
            entries[1].restoredSql,
        )
    }

    @Test
    fun `format insert values and preserve keyword inside string literal`() {
        val text = """
            Preparing: insert into audit_log(message, level) values (?, ?)
            Parameters: value from where and select(String), info(String)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals(
            """
            insert into audit_log(message, level)
            values ('value from where and select', 'info')
            """.trimIndent(),
            entry.restoredSql,
        )
    }

    @Test
    fun `format join and nested conditions`() {
        val text = """
            Preparing: select u.id, u.name from user u left join role r on u.role_id = r.id where (u.enabled = ? and u.deleted = ?) or r.code = ? order by u.name
            Parameters: true(Boolean), false(Boolean), admin(String)
        """.trimIndent()

        val entry = parser.parse(text, LogOrigin.MANUAL).single()

        assertEquals(
            """
            select u.id,
                u.name
            from user u
            left join role r
            on u.role_id = r.id
            where (u.enabled = true
                and u.deleted = false)
                or r.code = 'admin'
            order by u.name
            """.trimIndent(),
            entry.restoredSql,
        )
    }
}
