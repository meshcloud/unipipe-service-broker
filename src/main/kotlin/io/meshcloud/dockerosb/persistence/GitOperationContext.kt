package io.meshcloud.dockerosb.persistence

import org.springframework.stereotype.Service
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock

/**
 * Design: Provides a simple means to synchronize/serialize all git repository interactions.
 * Only one [GitOperationContext] can be active at a single time.
 *
 * Typical git interactions should be fairly short-lived and therefore queuing them should be fine.
 * Of course this limits the theoretical throughput of the system. However a typical unipipe deployment does not have to
 * handle a lot of requests per second so this should not be much of a problem. If it becomes a problem, we can optimize
 * this further at the cost of some additional complexity (e.g. separate read/write paths).
 */
@Service
class GitOperationContextFactory(
    private val gitHandler: GitHandler,
    private val yamlHandler: YamlHandler
) {

  // we have exactly one git operation that may be active at any time
  private val lock = ReentrantLock(true)

  fun acquireContext(): GitOperationContext {
    assert(!lock.isHeldByCurrentThread) {
      "Tried to acquire a ${GitOperationContext::class.simpleName} while the current thread has already acquired one. This is a coding error as it could lead to deadlock/double-release situations."
    }

    lock.lock()

    return GitOperationContext(
        yamlHandler,
        gitHandler
    ) { instance -> releaseContext(instance) }
  }

  fun releaseContext(context: GitOperationContext) {
    lock.unlock()
  }
}

class GitOperationContext(
    val yamlHandler: YamlHandler,
    private val gitHandler: GitHandler,
    private val onClose: (GitOperationContext) -> Unit
) : Closeable {

  override fun close() {
    onClose(this)
  }

  /**
   * Consumers should use this to signal that they wish to operate on the latest remote changes available.
   * See [GitHandler.pullFastForwardOnly]
   *
   * Design:
   * Only local commits are batched and periodically synced to the remote, see notes on [ScheduledPushHandler].
   * Remote commits on the other hand are not periodically pulled (unless they are synced as part of a scheduled push as described above).
   * The reason we do _not_ pull periodically is that we assume unipipe pipelines are in "steady" state most of the time,
   * meaning all services are provisioned and there's no activity ongoing. Periodically pulling would therefore be a waste
   * of resources both locally as well as on the remote repository as the vast majority of pulls would contain no changes.
   *
   * Instead we use this method to signal that we want to fetch any changes from the remote on an "on-demand" basis.
   * This is roughly equivalent to a `git pull --ff-only` and should typically be a quick and inexpensive operation.
   *
   * TODO: we may want to implement rate limiting etc. in here
   */
  fun attemptToRefreshRemoteChanges(){
    gitHandler.pullFastForwardOnly()
  }

  /**
   * See [GitHandler.synchronizeWithRemoteRepository]
   */
  fun synchronizeWithRemoteRepository(){
    gitHandler.synchronizeWithRemoteRepository()
  }

  fun buildServiceInstanceRepository(): ServiceInstanceRepository {
    return ServiceInstanceRepository(yamlHandler, gitHandler)
  }

  fun buildServiceInstanceBindingRepository(): ServiceInstanceBindingRepository {
    return ServiceInstanceBindingRepository(yamlHandler, gitHandler)
  }

  fun buildCatalogRepository(): CatalogRepository {
    return CatalogRepository(yamlHandler, gitHandler)
  }
}


