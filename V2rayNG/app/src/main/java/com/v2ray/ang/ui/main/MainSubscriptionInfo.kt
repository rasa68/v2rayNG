package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.handler.SubscriptionInfoParser
import com.v2ray.ang.util.Utils

/** Time left until a subscription expires. */
internal sealed interface SubscriptionTimeLeft {
    data object NoExpiry : SubscriptionTimeLeft
    data object Expired : SubscriptionTimeLeft
    data class Left(val days: Long, val hours: Long, val minutes: Long) : SubscriptionTimeLeft
}

/** Locale-neutral values the subscription information box renders. */
internal data class SubscriptionInfoUiModel(
    val title: String?,
    val usedBytes: Long,
    /** Traffic quota in bytes; 0 means unlimited. */
    val totalBytes: Long,
    /** Null when the quota is unlimited. */
    val leftBytes: Long?,
    /** Used share of the quota in 0..1; null when the quota is unlimited. */
    val trafficFraction: Float?,
    val expireMillis: Long?,
    val timeLeft: SubscriptionTimeLeft,
    val supportUrl: String?,
    val webPageUrl: String?,
) {
    val trafficLow: Boolean get() = trafficFraction != null && trafficFraction >= TRAFFIC_WARNING_FRACTION

    companion object {
        const val TRAFFIC_WARNING_FRACTION = 0.9f
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L

internal fun buildSubscriptionInfoUiModel(
    info: SubscriptionUsageInfo,
    nowMillis: Long,
): SubscriptionInfoUiModel {
    val used = info.used
    val unlimited = info.total <= 0L
    val expireMillis = info.expire.takeIf { it > 0L }?.let { it * 1000L }
    val timeLeft = when {
        expireMillis == null -> SubscriptionTimeLeft.NoExpiry
        expireMillis <= nowMillis -> SubscriptionTimeLeft.Expired
        else -> {
            val totalMinutes = (expireMillis - nowMillis) / MILLIS_PER_MINUTE
            SubscriptionTimeLeft.Left(
                days = totalMinutes / (MINUTES_PER_HOUR * HOURS_PER_DAY),
                hours = totalMinutes / MINUTES_PER_HOUR % HOURS_PER_DAY,
                minutes = totalMinutes % MINUTES_PER_HOUR,
            )
        }
    }
    return SubscriptionInfoUiModel(
        title = info.title,
        usedBytes = used,
        totalBytes = if (unlimited) 0L else info.total,
        leftBytes = if (unlimited) null else (info.total - used).coerceAtLeast(0L),
        trafficFraction = if (unlimited) null else (used.toDouble() / info.total).toFloat().coerceIn(0f, 1f),
        expireMillis = expireMillis,
        timeLeft = timeLeft,
        supportUrl = info.supportUrl,
        webPageUrl = info.webPageUrl,
    )
}

/**
 * Fixed box on top of a subscription group showing the account information published by the
 * subscription panel: traffic, expiry, support link and the panel's information page.
 */
@Composable
internal fun SubscriptionInfoCard(
    info: SubscriptionUsageInfo,
    fallbackTitle: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = remember(info) { buildSubscriptionInfoUiModel(info, System.currentTimeMillis()) }
    val title = model.title ?: fallbackTitle

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                model.supportUrl?.let { url ->
                    IconButton(onClick = { onOpenLink(url) }, Modifier.size(36.dp)) {
                        Icon(
                            painterResource(
                                if (SubscriptionInfoParser.isTelegramLink(url)) {
                                    R.drawable.ic_telegram_24dp
                                } else {
                                    R.drawable.ic_feedback_24dp
                                }
                            ),
                            stringResource(R.string.sub_info_open_support),
                            Modifier.size(24.dp),
                        )
                    }
                }
                model.webPageUrl?.let { url ->
                    IconButton(onClick = { onOpenLink(url) }, Modifier.size(36.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_about_24dp),
                            stringResource(R.string.sub_info_open_page),
                            Modifier.size(24.dp),
                        )
                    }
                }
            }

            // One focus/semantics node for the read-only figures so TalkBack reads them in one go.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                TrafficRows(model)
                Spacer(Modifier.height(6.dp))
                ExpiryRow(model)
            }
        }
    }
}

@Composable
private fun TrafficRows(model: SubscriptionInfoUiModel) {
    val totalText = if (model.totalBytes > 0L) {
        model.totalBytes.toTrafficString()
    } else {
        stringResource(R.string.sub_info_unlimited)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.sub_info_traffic_used, model.usedBytes.toTrafficString(), totalText),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        model.leftBytes?.let { left ->
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.sub_info_traffic_left, left.toTrafficString()),
                style = MaterialTheme.typography.bodyMedium,
                color = if (model.trafficLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    model.trafficFraction?.let { fraction ->
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .clearAndSetSemantics {},
            color = if (model.trafficLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ExpiryRow(model: SubscriptionInfoUiModel) {
    val expiresText = model.expireMillis?.let {
        stringResource(R.string.sub_info_expires, Utils.formatTimestamp(it))
    } ?: stringResource(R.string.sub_info_no_expiry)
    val timeLeft = model.timeLeft
    val timeLeftText = when (timeLeft) {
        SubscriptionTimeLeft.NoExpiry -> null
        SubscriptionTimeLeft.Expired -> stringResource(R.string.sub_info_expired)
        is SubscriptionTimeLeft.Left -> when {
            timeLeft.days > 0 -> stringResource(R.string.sub_info_time_left_dh, timeLeft.days, timeLeft.hours)
            timeLeft.hours > 0 -> stringResource(R.string.sub_info_time_left_hm, timeLeft.hours, timeLeft.minutes)
            else -> stringResource(R.string.sub_info_time_left_m, timeLeft.minutes)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = expiresText,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (timeLeftText != null) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = timeLeftText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (timeLeft == SubscriptionTimeLeft.Expired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}
