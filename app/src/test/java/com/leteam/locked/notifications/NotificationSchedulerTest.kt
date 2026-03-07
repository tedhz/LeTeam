package com.leteam.locked.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class NotificationSchedulerTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockAlarmManager: AlarmManager = mockk(relaxed = true)
    private val mockPendingIntent: PendingIntent = mockk(relaxed = true)

    private lateinit var scheduler: NotificationScheduler

    @Before
    fun setUp() {
        every { mockContext.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager

        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any(), any())
        } returns mockPendingIntent

        scheduler = NotificationScheduler(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleDailyReminder sets repeating alarm`() {
        val hour = 18
        val minute = 30

        scheduler.scheduleDailyReminder(hour, minute)

        verify {
            PendingIntent.getBroadcast(
                mockContext,
                0,
                any<Intent>(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        verify {
            mockAlarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                any(),
                AlarmManager.INTERVAL_DAY,
                mockPendingIntent
            )
        }
    }

    @Test
    fun `cancelReminder cancels pending intent in AlarmManager`() {
        scheduler.cancelReminder()

        verify {
            PendingIntent.getBroadcast(
                mockContext,
                0,
                any<Intent>(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        verify {
            mockAlarmManager.cancel(mockPendingIntent)
        }
    }
}