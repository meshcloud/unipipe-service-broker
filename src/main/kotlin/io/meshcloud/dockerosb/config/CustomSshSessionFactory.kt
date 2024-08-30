package io.meshcloud.dockerosb.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class CustomSshSessionFactory(
    private val sshKey: String
) : JschConfigSessionFactory() {

  override fun configure(hc: OpenSshConfig.Host, session: Session) {
    session.setConfig("StrictHostKeyChecking", "no")

    /*
      Because of End of SSH-RSA support for Azure Repos (https://devblogs.microsoft.com/devops/ssh-rsa-deprecation/)
      we need to explicitly put "ssh-rsa" last in the list in server_host_key so that it is not picked up first.
    */
    session.setConfig("server_host_key",
        session.getConfig("server_host_key").
        split(",").partition { it == "ssh-rsa" }
            .let { it.second + it.first }.joinToString(","))
  }

  @Throws(JSchException::class)
  override fun getJSch(hc: OpenSshConfig.Host, fs: FS): JSch {
    val keyPath = "tmp/ssh_key"
    val contentStartIndex = sshKey.indexOf("-----", 1) + 5
    val contentEndIndex = sshKey.indexOf("-----", 50) + 5 // use an offset, that is somewhere within the content of the key
    val content = sshKey.substring(contentStartIndex, contentEndIndex).replace(" ", "\n")
    val formattedKey = sshKey.substring(0, contentStartIndex) + content + sshKey.substring(contentEndIndex, sshKey.length - 1)

    val keyFile = File(keyPath)
    Files.createDirectories(Paths.get(keyFile.parent))
    keyFile.writeText(formattedKey)

    val jsch = super.getJSch(hc, fs)
    jsch.removeAllIdentity()
    jsch.addIdentity(keyPath)
    return jsch
  }
}
