package io.meshcloud.dockerosb.persistence

import java.io.File

/**
 * Note: consumers should use this only via a [GitOperationContext]
 */
interface GitHandler {

  fun pullFastForwardOnly()

  fun commitAllChanges(commitMessage: String)

  fun synchronizeWithRemoteRepository()

  fun fileInRepo(path: String): File

  fun instancesDirectory(): File

  fun getLastCommitMessage(): String

  fun instanceYmlRelativePath(instanceId: String): String

  fun instanceDirectoryRelativePath(instanceId: String): String

  fun bindingDirectoryRelativePath(instanceId: String, bindingId: String): String

  fun bindingYmlRelativePathInRepo(instanceId: String, bindingId: String): String

  fun filesInRepo(instanceFolderPath: String): List<File>
}
