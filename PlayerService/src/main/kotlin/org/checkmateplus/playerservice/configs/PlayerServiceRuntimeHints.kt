package org.checkmateplus.playerservice.configs

import jakarta.servlet.http.HttpServletRequest
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class PlayerServiceRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.proxies().registerJdkProxy(HttpServletRequest::class.java)
    }
}