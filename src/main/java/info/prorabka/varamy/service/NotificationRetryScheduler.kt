package info.prorabka.varamy.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Component
class NotificationRetryScheduler(
    @Lazy private val notificationService: NotificationService  // <-- ДОБАВЛЯЕМ @Lazy
) {

    private val log = LoggerFactory.getLogger(NotificationRetryScheduler::class.java)
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val retryCounts: MutableMap<Long, Int> = ConcurrentHashMap()

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY: Long = 30 // секунд
    }

    fun scheduleRetry(notificationId: Long) {
        val retryCount = retryCounts.getOrDefault(notificationId, 0)

        if (retryCount >= MAX_RETRIES) {
            retryCounts.remove(notificationId)
            log.warn("Превышено количество попыток отправки уведомления {}", notificationId)
            return
        }

        retryCounts[notificationId] = retryCount + 1
        log.info("⏳ Запланирована повторная отправка уведомления {}, попытка {}", notificationId, retryCount + 1)

        scheduler.schedule({
            try {
                notificationService.resendNotification(notificationId)
                retryCounts.remove(notificationId)
                log.info("✅ Уведомление {} успешно отправлено повторно", notificationId)
            } catch (e: Exception) {
                log.error("❌ Ошибка при повторной отправке уведомления {}, попытка {}",
                    notificationId, retryCount + 1, e)
                scheduleRetry(notificationId)
            }
        }, RETRY_DELAY, TimeUnit.SECONDS)
    }
}