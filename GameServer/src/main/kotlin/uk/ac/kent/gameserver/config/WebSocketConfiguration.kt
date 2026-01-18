package uk.ac.kent.gameserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
class WebSocketConfiguration {

    @Bean
    fun createWebSocketContainer() = ServletServerContainerFactoryBean().apply {
        maxTextMessageBufferSize = 8192
        maxBinaryMessageBufferSize = 8192
    }
}