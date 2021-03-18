package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.GitConfig
import mu.KotlinLogging
import org.eclipse.jgit.api.RebaseResult
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

private val log = KotlinLogging.logger {}

@Service
class SynchronizedGitHandlerWrapper(gitConfig: GitConfig) : GitHandler(gitConfig) {

  private val readWriteLock = ReentrantReadWriteLock()
  private val readLock = readWriteLock.readLock()
  private val writeLock = readWriteLock.writeLock()
  private val commitSyncLock = ReentrantLock()

  private val retryTemplate = RetryTemplate().apply {
    setBackOffPolicy(FixedBackOffPolicy().apply { backOffPeriod = 5 * 1000 })
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = 3 })
  }

  /**
   * We assume there are new commits in git, in case there are some.
   * This prevents commits "laying around" at start time until a new
   * commits would set this flag.
   * TODO we can determine this exactly here instead.
   */
  private val hasNewCommits = AtomicBoolean(true)

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   * Additionally acquire commitSync lock to not interfere with other commits.
   */
  fun safeCommit(filePaths: List<String>, commitMessage: String) {
    readLock.lock()
    commitSyncLock.lock()
    try {
      super.commit(filePaths, commitMessage)
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
  fun rebaseAndPushAllCommittedChanges() {
    writeLock.lock()
    try {
      lockedRebaseAndPushAllCommittedChanges()
    } catch(ex: Exception) {
      log.error { "Failed to rebase and commit changes." }
    } finally {
      writeLock.unlock()
    }
  }

  private fun lockedRebaseAndPushAllCommittedChanges() {
    if (!hasNewCommits.get()) {
      return
    }

    var rebaseResult: RebaseResult
    var retryRebase: Boolean

    do {
      retryRebase = false

      /**
       * Retry rebase command execution, in case something goes wrong, not in terms of conflicts or similar,
       * but in case the rebase command itself could be properly executed, e.g. remote not reachable etc.
       * In case the rebase fails for all attempts an exception is thrown and we won't continue.
       */
      rebaseResult = retryTemplate.execute<RebaseResult, Exception> { ctx ->
        try {
          super.rebase()
        } catch (ex: Exception) {
          log.error { "Rebase attempt #${ctx.retryCount}: ${ex.message}." }
          throw ex
        }
      }

      if (!rebaseResult.status.isSuccessful) {
        when (rebaseResult.status) {

          // This should never occur, but we can resolve it easily.
          RebaseResult.Status.UNCOMMITTED_CHANGES -> {
            super.cleanUp()
            retryRebase = true
          }

          else -> {
            // TODO any more cases we can solve automatically?
            // RebaseResult.Status.STOPPED
            // RebaseResult.Status.FAILED
            // RebaseResult.Status.CONFLICTS
            // should never occur:
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
      retryTemplate.execute<Unit, Exception> { ctx ->
        try {
          super.push()
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

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   */
  override fun pull() {
    readLock.lock()
    try {
      super.pull()
    } finally {
      readLock.unlock()
    }
  }
}