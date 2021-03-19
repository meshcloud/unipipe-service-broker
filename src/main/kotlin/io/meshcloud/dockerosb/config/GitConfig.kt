package io.meshcloud.dockerosb.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class GitConfig(
    @Value("\${git.local-path}")
    val localPath: String,

    @Value("\${git.remote:#{null}}")
    val remote: String?,

    @Value("\${git.remote-branch}")
    val remoteBranch: String,

    @Value("\${git.ssh-key:#{null}}")
    val sshKey: String?,

    @Value("\${git.username:#{null}}")
    val username: String?,

    @Value("\${git.password:#{null}}")
    val password: String?
) {
    fun hasRemoteConfigured(): Boolean {
        return this.remote != null
    }
}
