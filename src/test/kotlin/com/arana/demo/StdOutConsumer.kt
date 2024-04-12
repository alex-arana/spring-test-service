package com.arana.demo

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.BaseConsumer
import org.testcontainers.containers.output.OutputFrame

class StdOutConsumer<SELF : BaseConsumer<SELF>>(private val prefix: String) : BaseConsumer<SELF>() {
    override fun accept(outputFrame: OutputFrame) {
        with(outputFrame) {
            when (type) {
                OutputFrame.OutputType.STDOUT -> println("$prefix | $utf8StringWithoutLineEnding")
                OutputFrame.OutputType.STDERR -> System.err.println("$prefix | $utf8StringWithoutLineEnding")
                else -> Unit
            }
        }
    }
}

fun <SELF : GenericContainer<SELF>> SELF.withStdOutConsumer(prefix: String = ""): SELF {
    val logPrefix = prefix.ifEmpty { dockerImageName.substringBefore(":") }
    return withLogConsumer(StdOutConsumer(logPrefix))
}
