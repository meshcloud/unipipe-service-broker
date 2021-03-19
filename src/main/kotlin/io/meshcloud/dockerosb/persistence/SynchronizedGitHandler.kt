package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.GitConfig
import io.meshcloud.dockerosb.config.RetryConfig
import io.meshcloud.dockerosb.exceptions.GitCommandException
import io.meshcloud.dockerosb.service.GitHandler
import io.meshcloud.dockerosb.service.GitHandler.Companion.getGit
import mu.KotlinLogging
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

private val log = KotlinLogging.logger {}

@Service
class SynchronizedGitHandler(
  private val gitConfig: GitConfig,
  private val retryConfig: RetryConfig
) : GitHandler {

  private val readWriteLock = ReentrantReadWriteLock(true)
  private val readLock = readWriteLock.readLock()
  private val writeLock = readWriteLock.writeLock()
  private val commitSyncLock = ReentrantLock()

  /**
   * We assume there are new commits in git, in case there are some.
   * This prevents commits "laying around" at start time until a new
   * commit would set this flag.
   * TODO we can determine this exactly here instead.
   */
  private val hasNewCommits = AtomicBoolean(true)

  private val remoteWriteRetryTemplate = RetryTemplate().apply {
    setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = retryConfig.remoteWriteBackOffDelay })
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = retryConfig.remoteWriteAttempts })
  }

  private val gitLockRetryTemplate = RetryTemplate().apply {
    setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = retryConfig.gitLockBackOffDelay })
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = retryConfig.gitLockAttempts })
  }

  private val git = getGit(gitConfig)

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   */
  override fun pull() {
    if (!gitConfig.hasRemoteConfigured()) {
      return
    }

    readLock.lock()
    try {
      internalPull()
    } finally {
      readLock.unlock()
    }
  }

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   * Additionally acquire commitSync lock to not interfere with other commits.
   */
  override fun commit(filePaths: List<String>, commitMessage: String) {
    readLock.lock()
    commitSyncLock.lock()
    try {
      withLockExceptionRetry { internalAdd(filePaths) }
      withLockExceptionRetry { internalCommit(commitMessage) }
      hasNewCommits.set(true)
    } finally {
      commitSyncLock.unlock()
      readLock.unlock()
    }
  }

  /**
   * This will acquire the writeLock so all parallel access to
   * the writeLock and any access to the readLock will be delayed.
   *
   * If there are no new commits, we do nothing to keep to amount
   * of unnecessary calls to git small.
   */
  override fun rebaseAndPushAllCommittedChanges() {
    if (!gitConfig.hasRemoteConfigured()) {
      return
    }

    writeLock.lock()
    try {
      internalRebaseAndPushAllCommittedChanges()
    } catch (ex: Exception) {
      log.error { "Failed to rebase and commit changes." }
    } finally {
      writeLock.unlock()
    }
  }

  override fun fileInRepo(path: String): File {
    return File(gitConfig.localPath, path)
  }

  override fun getLastCommitMessage(): String {
    return git.log().setMaxCount(1).call().single().fullMessage
  }

  private fun internalRebaseAndPushAllCommittedChanges() {
    if (!hasNewCommits.get()) {
      return
    }

    log.info { "Pushing new commits." }

    var rebaseResult: RebaseResult
    var retryRebase: Boolean

    do {
      retryRebase = false

      /**
       * Retry rebase command execution, in case something goes wrong, not in terms of conflicts or similar,
       * but in case the rebase command itself could be properly executed, e.g. remote not reachable etc.
       * In case the rebase fails for all attempts an exception is thrown and we won't continue.
       */
      rebaseResult = remoteWriteRetryTemplate.execute<RebaseResult, Exception> { ctx ->
        try {
          git.rebase()
            .setUpstream(gitConfig.remoteBranch)
            .call()
        } catch (ex: Exception) {
          log.error { "Rebase attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }

      if (!rebaseResult.status.isSuccessful) {
        when (rebaseResult.status) {

          // This should never occur, but we can resolve it easily.
          RebaseResult.Status.UNCOMMITTED_CHANGES -> {
            cleanUp()
            retryRebase = true
          }

          else -> {
            // TODO any more cases we can solve automatically?
            // RebaseResult.Status.STOPPED
            // RebaseResult.Status.FAILED
            // RebaseResult.Status.CONFLICTS
            // and these should never occur here:
            // RebaseResult.Status.ABORTED
            // RebaseResult.Status.EDIT
          }
        }
      }
    } while (retryRebase)

    if (rebaseResult.status.isSuccessful) {
      log.info { "Successfully rebased." }

      /**
       * Retry push command execution, in case something goes wrong, e.g. remote not reachable etc.
       * In case the push fails for all attempts an exception is thrown and we won't continue.
       */
      remoteWriteRetryTemplate.execute<Unit, Exception> { ctx ->
        try {
          push()
          hasNewCommits.set(false)
          log.info { "Successfully pushed all commits." }
        } catch (ex: Exception) {
          log.error { "Push attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }
    } else {
      /**
       * We know the rebase cannot be done automatically here.
       * So there are conflicts that need to be resolved.
       * TODO is there any strategy to resolve this?
       */
      log.error { "Could not rebase and push commits." }
    }
  }

  private fun internalAdd(filePaths: List<String>) {
    val addCommand = git.add()
    filePaths.forEach { addCommand.addFilepattern(it) }
    addCommand.call()
  }

  private fun internalCommit(commitMessage: String) {
    git.commit()
      .setMessage("OSB API: $commitMessage")
      .setAuthor("UniPipe OSB API", "osb@meshcloud.io")
      .call()
  }

  private fun push() {
    val pushCommand = git.push()
    gitConfig.username?.let {
      pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
    }
    pushCommand.call()
  }

  private fun cleanUp() {
    git.clean().setForce(true).setCleanDirectories(true).call()
    git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
  }

  private fun internalPull() {
    val pullCommand = git.pull()
      .setRemote("origin")
      .setRemoteBranchName(gitConfig.remoteBranch)

    gitConfig.username?.let {
      pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(gitConfig.username, gitConfig.password))
    }
    val pullResult = pullCommand.call()

    if (!pullResult.isSuccessful) {
      throw GitCommandException("Git Pull failed.", null)
    }
  }

  private fun withLockExceptionRetry(code: () -> Unit) {
    gitLockRetryTemplate.execute<Unit, LockFailedException> {
      try {
        code()
      } catch (ex: LockFailedException) {
        log.warn { "There was a LockFailedException, will retry." }
        throw ex
      }
    }
  }
}