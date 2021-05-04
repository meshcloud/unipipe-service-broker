package io.meshcloud.dockerosb.persistence

import org.springframework.stereotype.Service
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock

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
}

