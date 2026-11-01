package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.verticalScrollbar

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private val primaryDrawerItems = listOf(
    MainDestination.Subscriptions,
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.UserAssets,
    MainDestination.Settings
)

private val drawerItems = primaryDrawerItems + listOf(
    MainDestination.Promotion,
    MainDestination.Logcat,
    MainDestination.CheckUpdate,
    MainDestination.BackupRestore,
    MainDestination.About
)

/**
 * Full-screen menu: a back arrow and the app name with its version on top, then the destinations in
 * two groups separated by a divider. The arrow closes the menu and returns to the config lists.
 */
@Composable
fun MainDrawerContent(onClose: () -> Unit, onNavigate: (MainDestination) -> Unit) {
    val scrollState = rememberScrollState()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = MaterialTheme.colorScheme
    // The trailing LRM keeps the closing parenthesis on the right side of the version in RTL languages.
    val title = buildAnnotatedString {
        append(stringResource(R.string.app_name))
        withStyle(
            SpanStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant
            )
        ) {
            append(" (${BuildConfig.VERSION_NAME})\u200E")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24dp),
                        contentDescription = stringResource(R.string.acc_back),
                        // The drawable is not auto-mirrored: point it the other way in RTL.
                        modifier = Modifier.graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AppDivider(Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
            ) {
                drawerItems.forEachIndexed { index, item ->
                    if (index == primaryDrawerItems.size) {
                        AppDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item) }
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colors.onSurface
                        )
                        Spacer(Modifier.width(24.dp))
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}
