package io.meshcloud.dockerosb.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig(
    @Value("\${app.basicauthusername}")
    val basicAuthUsername: String,

    @Value("\${app.basicauthpassword}")
    val basicAuthPassword: String
)
