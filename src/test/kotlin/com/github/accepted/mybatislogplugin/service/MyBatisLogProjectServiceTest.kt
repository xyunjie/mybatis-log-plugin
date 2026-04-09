package com.github.accepted.mybatislogplugin.service

import com.github.accepted.mybatislogplugin.model.LogOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyBatisLogProjectServiceTest {
    private val service = MyBatisLogProjectService()

    @Test
    fun `keep only latest 20 auto entries`() {
        repeat(25) { index ->
            service.appendLine("Preparing: select * from user where id = ?", LogOrigin.AUTO)
            service.appendLine("Parameters: ${index + 1}(Integer)", LogOrigin.AUTO)
        }

        val autoEntries = service.getEntries(origin = LogOrigin.AUTO)

        assertEquals(20, autoEntries.size)
        assertTrue(autoEntries.first().restoredSql.contains("where id = 25"))
        assertTrue(autoEntries.last().restoredSql.contains("where id = 6"))
    }

    @Test
    fun `manual entries are not limited by auto retention`() {
        repeat(25) { index ->
            service.appendLine("Preparing: select * from user where id = ?", LogOrigin.AUTO)
            service.appendLine("Parameters: ${index + 1}(Integer)", LogOrigin.AUTO)
        }
        repeat(3) { index ->
            service.parseManual(
                """
                Preparing: select * from role where id = ?
                Parameters: ${index + 1}(Integer)
                """.trimIndent(),
            )
        }

        val autoEntries = service.getEntries(origin = LogOrigin.AUTO)
        val manualEntries = service.getEntries(origin = LogOrigin.MANUAL)

        assertEquals(20, autoEntries.size)
        assertEquals(3, manualEntries.size)
    }
}
