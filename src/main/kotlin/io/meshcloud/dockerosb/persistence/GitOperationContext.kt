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
    val gitHandler: GitHandler,
    private val onClose: (GitOperationContext) -> Unit
) : Closeable {

  override fun close() {
    onClose(this)
  }

  fun buildServiceInstanceRepository(): ServiceInstanceRepository {
    return ServiceInstanceRepository(yamlHandler, gitHandler)
  }

}


