package io.meshcloud.dockerosb.persistence

import java.io.Closeable

class GitOperationContext(
    private val yamlHandler: YamlHandler,
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


