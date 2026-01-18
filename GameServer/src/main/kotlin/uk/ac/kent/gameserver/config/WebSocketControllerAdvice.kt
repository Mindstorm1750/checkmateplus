package uk.ac.kent.gameserver.config

import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.ControllerAdvice
import java.security.Principal

@ControllerAdvice
class WebSocketControllerAdvice(private val messagingTemplate: SimpMessagingTemplate) {
    @MessageExceptionHandler
    fun handleException(
        ex: Exception,
        principal: Principal //refers to a user, i.e. where it came from
    ) {
        messagingTemplate.convertAndSendToUser(
            principal.name,
            "/queue/errors",
            ex.message ?: "Unknown error"
        )
    }
}
