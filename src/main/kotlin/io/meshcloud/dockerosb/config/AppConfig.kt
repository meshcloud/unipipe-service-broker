package io.meshcloud.dockerosb.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig(
    @Value("\${app.basic-auth-username}")
    val basicAuthUsername: String,

    @Value("\${app.basic-auth-password}")
    val basicAuthPassword: String
)
