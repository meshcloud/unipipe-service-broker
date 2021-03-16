package io.meshcloud.dockerosb.persistence

import io.meshcloud.dockerosb.config.GitConfig
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class SynchronizedGitHandlerWrapper(gitConfig: GitConfig) : GitHandler(gitConfig) {

  private val readWriteLock = ReentrantReadWriteLock()
  private val readLock = readWriteLock.readLock()
  private val writeLock = readWriteLock.writeLock()
  private val commitSyncLock = ReentrantLock()

  /**
   * We assume there are new commits in git, in case there are some.
   * This prevents commits "laying around" at start time until a new
   * commits would set this flag.
   */
  private val hasNewCommits = AtomicBoolean(true)

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   * Additionally acquire commitSync lock to not interfere with other commits.
   */
  fun safeCommit(filePaths: List<String>, commitMessage: String) {
    readLock.lock()
    commitSyncLock.lock()
    super.commit(filePaths, commitMessage)
    hasNewCommits.set(true)
    commitSyncLock.unlock()
    readLock.unlock()
  }

  /**
   * This will acquire the writeLock so all parallel access to
   * the writeLock and any access to the readLock will be delayed.
   *
   * If there are no new commits, we do nothing to keep to amount
   * of unnecessary calls to git small.
   */
  fun rebaseAndPushAllCommittedChanges(): Boolean {
    if (!hasNewCommits.getAndSet(false)) {
      return false
    }

    writeLock.lock()
    try {
      //do not require read lock here as we'd never acquire it here.
      super.pull(true)
      super.pushAllOpenChanges()

    } catch (ex: Exception) {
      //TODO what to do in this case?

    } finally {
      writeLock.unlock()
    }
    return true
  }

  /**
   * Acquire readLock to be sure that we don't conflict with the writeLock.
   */
  override fun pull(doRebase: Boolean) {
    readLock.lock()
    super.pull(doRebase)
    readLock.unlock()
  }
}