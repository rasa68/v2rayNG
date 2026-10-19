package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainSubscriptionInfoTest {
    private val now = 1_000_000_000_000L // ms

    @Test
    fun limitedTrafficComputesLeftAndFraction() {
        val model = buildSubscriptionInfoUiModel(SubscriptionUsageInfo(upload = 100, download = 300, total = 1000), now)
        assertEquals(400, model.usedBytes)
        assertEquals(600L, model.leftBytes)
        assertEquals(0.4f, model.trafficFraction!!, 0.0001f)
        assertFalse(model.trafficLow)
    }

    @Test
    fun unlimitedTrafficHasNoLeftOrFraction() {
        val model = buildSubscriptionInfoUiModel(SubscriptionUsageInfo(download = 5), now)
        assertEquals(0L, model.totalBytes)
        assertNull(model.leftBytes)
        assertNull(model.trafficFraction)
        assertFalse(model.trafficLow)
    }

    @Test
    fun overUsageIsClampedAndFlaggedLow() {
        val model = buildSubscriptionInfoUiModel(SubscriptionUsageInfo(download = 2000, total = 1000), now)
        assertEquals(0L, model.leftBytes)
        assertEquals(1f, model.trafficFraction!!, 0.0001f)
        assertTrue(model.trafficLow)
    }

    @Test
    fun timeLeftIsSplitIntoDaysHoursMinutes() {
        val expire = now / 1000 + (2 * 86_400 + 3 * 3_600 + 4 * 60) + 30
        val model = buildSubscriptionInfoUiModel(SubscriptionUsageInfo(expire = expire), now)
        assertEquals(SubscriptionTimeLeft.Left(2, 3, 4), model.timeLeft)
        assertEquals(expire * 1000, model.expireMillis)
    }

    @Test
    fun pastExpiryIsExpiredAndZeroMeansNoExpiry() {
        assertEquals(SubscriptionTimeLeft.Expired, buildSubscriptionInfoUiModel(SubscriptionUsageInfo(expire = now / 1000 - 1), now).timeLeft)
        val none = buildSubscriptionInfoUiModel(SubscriptionUsageInfo(expire = 0), now)
        assertEquals(SubscriptionTimeLeft.NoExpiry, none.timeLeft)
        assertNull(none.expireMillis)
    }
}
