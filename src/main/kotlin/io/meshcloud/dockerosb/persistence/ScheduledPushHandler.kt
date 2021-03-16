package io.meshcloud.dockerosb.persistence

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ScheduledPushHandler(val gitAccess: SynchronizedGitHandlerWrapper) {

  @Scheduled(
    initialDelayString = "\${scheduling.push.initial-delay}",
    fixedDelayString = "\${scheduling.push.pause-delay}"
  )
  fun pushTask() {
    log.info { "Checking for new commits:" }
    val pushed = gitAccess.rebaseAndPushAllCommittedChanges()
    if(!pushed) {
      log.info { "There were none." }
    } else {
      log.info { "New commits have been pushed to git." }
    }
  }
}