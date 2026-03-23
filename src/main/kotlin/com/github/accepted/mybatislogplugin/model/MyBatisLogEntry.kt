package com.github.accepted.mybatislogplugin.model

import java.time.Instant

data class MyBatisLogEntry(
    val id: String,
    val timestamp: Instant,
    val origin: LogOrigin,
    val sourceText: String,
    val rawPreparing: String,
    val rawParameters: String,
    val restoredSql: String,
    val rawBlock: String,
)
