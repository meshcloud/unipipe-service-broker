package io.meshcloud.dockerosb.persistence

import java.io.File

interface GitHandler {

  fun pullFastForwardOnly()

  fun commitAllChanges(commitMessage: String)

  fun synchronizeWithRemoteRepository()

  fun fileInRepo(path: String): File

  fun getLastCommitMessage(): String
}
