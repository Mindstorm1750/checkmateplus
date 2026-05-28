package org.checkmateplus.voicecommandservice.config

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.springboot.OpenAIClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OpenAIConfig {
    @Bean
    fun customizer(): OpenAIClientCustomizer {
        return OpenAIClientCustomizer { builder: OpenAIOkHttpClient.Builder? -> builder!!.maxRetries(3) }
    }
}