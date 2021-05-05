package io.meshcloud.dockerosb.persistence

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Design:
 * We only push git changes periodically to the remote.
 *
 * Broadly speaking this implies we have the following tradeoffs:
 *
 * - availability: We have only quick local filesystem git interactions on the critical path for serving requests.
 *                 we can also serve requests if the remote git repository is temporarily not available
 *
 * - consistency: Interactions to the remote repository (e.g. the CI/CD tool or an operator updating files) need to be
 *                merged during synchronization. This can potentially lead to conflicts that we cannot automatically
 *                resolve. To avoid consistency issues, operators should take care to assign a clear ownership to files
 *                in the repository (e.g. unipipe-osb owns instance.yml's while CI/CD pipeline owns generated IaC files),
 *
 *                Another potential problem is that writes can get lost if unipipe-osb crashes before syncing a change
 *                to the remote git repository. To mitigate this operators could mount the local git repository path
 *                onto a persistent storage (e.g. via volume mounts).
 *
 * - partition tolerance: unipipe-osb should not be clustered, as multiple nodes could diverge very far from another.
 *                        Besides being more expensive to run, a true HA setup is also usually not needed since operators
 *                        typically run unipipe-osb in a container orchestrator and that can take care of restarting
 *                        failed instances with quick turnaround.
 */
@Component
class ScheduledPushHandler(val gitOperationContextFactory: GitOperationContextFactory) {

  @Scheduled(
      initialDelayString = "\${scheduling.push.initial-delay}",
      fixedDelayString = "\${scheduling.push.pause-delay}"
  )
  fun pushTask() {
    gitOperationContextFactory.acquireContext().use { context ->
      context.synchronizeWithRemoteRepository()
    }
  }
}