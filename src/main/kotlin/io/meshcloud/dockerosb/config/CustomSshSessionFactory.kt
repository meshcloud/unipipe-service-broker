package io.meshcloud.dockerosb.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.util.FS
import java.io.File

class CustomSshSessionFactory(
    private val sshKey: String
) : JschConfigSessionFactory() {

  override fun configure(hc: OpenSshConfig.Host, session: Session) {
    session.setConfig("StrictHostKeyChecking", "no")
  }

  @Throws(JSchException::class)
  override fun getJSch(hc: OpenSshConfig.Host, fs: FS): JSch {
    val keyPath = "tmp/ssh_key"
    val contentStartIndex = sshKey.indexOf("-----", 1) + 5
    val contentEndIndex = sshKey.indexOf("-----", 50) + 5 // use an offset, that is somewhere within the content of the key
    val content = sshKey.substring(contentStartIndex, contentEndIndex).replace(" ", "\n")
    val formattedKey = sshKey.substring(0, contentStartIndex) + content + sshKey.substring(contentEndIndex, sshKey.length - 1)
    FileUtils.writeStringToFile(File(keyPath), formattedKey)
    val jsch = super.getJSch(hc, fs)
    jsch.removeAllIdentity()
    jsch.addIdentity(keyPath)
    return jsch
  }
}