package io.meshcloud.dockerosb.service

import java.io.File

interface GitHandler {

  fun pull()

  fun commit(filePaths: List<String>, commitMessage: String)

  fun rebaseAndPushAllCommittedChanges()

  fun fileInRepo(path: String): File

  fun getLastCommitMessage(): String
}