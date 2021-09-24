package io.meshcloud.dockerosb

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Path

class RemoteGitFixture(
    private val repositoryRootPath: String,
    branch: String = "master"
) {
  // init remote git
  private val git = Git.init()
      .setDirectory(File(repositoryRootPath))
      .setInitialBranch(branch)
      .call()


  fun initWithCatalog(catalogPath: String) {
    FileUtils.copyFile(File(catalogPath), File("${repositoryRootPath}/catalog.yml"))
    commit("initial commit on remote")
  }

  fun commit(message: String) {
    git.add()
        .addFilepattern(".")
        .call()

    git.commit()
        .also { it.message = message }
        .call()
  }

  fun writeFile(relativePath: String, content: String) {
    Path.of(repositoryRootPath, relativePath)
        .toFile()
        .apply {
          parentFile.mkdirs()
          writeText(content)
        }
  }
}