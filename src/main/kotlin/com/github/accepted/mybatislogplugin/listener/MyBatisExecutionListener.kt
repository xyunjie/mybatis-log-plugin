package com.github.accepted.mybatislogplugin.listener

import com.github.accepted.mybatislogplugin.model.LogOrigin
import com.github.accepted.mybatislogplugin.service.MyBatisLogProjectService
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key

class MyBatisExecutionListener : ExecutionListener {
    override fun processStarted(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
    ) {
        attachListener(env.project ?: return, handler)
    }

    private fun attachListener(project: Project, handler: ProcessHandler) {
        val service = project.getService(MyBatisLogProjectService::class.java)
        handler.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) = Unit

            override fun processTerminated(event: ProcessEvent) = Unit

            override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) = Unit

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                event.text
                    .lineSequence()
                    .map { it.trimEnd('\r', '\n') }
                    .filter { it.isNotBlank() }
                    .forEach { service.appendLine(it, LogOrigin.AUTO) }
            }
        })
    }
}
