package io.meshcloud.dockerosb.persistence

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledPushHandler(val gitAccess: RetryingGitHandler) {

  @Scheduled(
    initialDelayString = "\${scheduling.push.initial-delay}",
    fixedDelayString = "\${scheduling.push.pause-delay}"
  )
  fun pushTask() {
    gitAccess.rebaseAndPushAllCommittedChanges()
  }
}