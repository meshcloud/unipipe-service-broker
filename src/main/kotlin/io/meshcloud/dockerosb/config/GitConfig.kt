package io.meshcloud.dockerosb.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class GitConfig(
    @Value("\${git.localpath}")
    val localPath: String,

    @Value("\${git.remote:#{null}}")
    val remote: String?,

    @Value("\${git.sshkey:#{null}}")
    val sshKey: String?,

    @Value("\${git.username:#{null}}")
    val username: String?,

    @Value("\${git.password:#{null}}")
    val password: String?
)
