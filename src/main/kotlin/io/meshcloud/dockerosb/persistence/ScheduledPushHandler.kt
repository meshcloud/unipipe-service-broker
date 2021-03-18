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
    log.info { "Pushing new commits if existing." }
    gitAccess.rebaseAndPushAllCommittedChanges()
  }
}