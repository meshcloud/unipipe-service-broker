package io.meshcloud.dockerosb

import io.meshcloud.dockerosb.config.GitConfig
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File

class GitRepoFixture : Closeable {

  val localGitPath = "tmp/test/git"

  val config = GitConfig(
      localPath = localGitPath,
      remote = null,
      sshKey = null,
      username = null,
      password = null
  )

  init {
    FileUtils.copyFile(File("src/test/resources/catalog.yml"), File("$localGitPath/catalog.yml"))
  }

  override fun close() {
    FileUtils.deleteDirectory(File(localGitPath))
  }
}