package org.nekomanga.presentation.screens.settings.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.getActivity
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.constants.Constants.DONT_KILL_MY_APP_URL
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.CleanDownloadsDialog
import org.nekomanga.presentation.components.dialog.ClearDatabaseDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import tachiyomi.core.network.PREF_DOH_360
import tachiyomi.core.network.PREF_DOH_ADGUARD
import tachiyomi.core.network.PREF_DOH_ALIDNS
import tachiyomi.core.network.PREF_DOH_CLOUDFLARE
import tachiyomi.core.network.PREF_DOH_CONTROLD
import tachiyomi.core.network.PREF_DOH_DNSPOD
import tachiyomi.core.network.PREF_DOH_GOOGLE
import tachiyomi.core.network.PREF_DOH_MULLVAD
import tachiyomi.core.network.PREF_DOH_NJALLA
import tachiyomi.core.network.PREF_DOH_QUAD101
import tachiyomi.core.network.PREF_DOH_QUAD9
import tachiyomi.core.network.PREF_DOH_SHECAN
import tachiyomi.core.util.system.setDefaultSettings

internal class AdvancedSettingsScreen(
    val preferences: PreferencesHelper,
    val networkPreferences: NetworkPreferences,
    val toastEvent: SharedFlow<UiText>,
    val clearNetworkCookies: () -> Unit,
    val cleanupDownloads: (Boolean, Boolean) -> Unit,
    val reindexDownloads: () -> Unit,
    val clearDatabase: (Boolean) -> Unit,
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.advanced

    @SuppressLint("BatteryLife")
    @Composable
    override fun getPreferences(): PersistentList<Preference> {
        val context = LocalContext.current

        LaunchedEffect(Unit) { toastEvent.collect { event -> context.toast(event) } }

        return persistentListOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = preferences.sendCrashReports(),
                title = stringResource(R.string.send_crash_report),
                subtitle = stringResource(R.string.helps_fix_bugs),
                onValueChanged = { enabled ->
                    FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
                    true
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.dump_crash_logs),
                subtitle = stringResource(R.string.saves_error_logs),
                onClick = {
                    context.getActivity()?.lifecycleScope?.launchIO {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = networkPreferences.verboseLogging(),
                title = stringResource(R.string.verbose_logging),
                subtitle = stringResource(R.string.verbose_logging_summary),
                onValueChanged = {
                    context.toast(R.string.requires_app_restart)
                    true
                },
            ),
            backgroundActivityGroup(context),
            networkGroup(context, clearNetworkCookies),
            dataGroup(cleanupDownloads, clearDatabase, reindexDownloads),
        )
    }

    @Composable
    fun backgroundActivityGroup(context: Context): Preference.PreferenceGroup {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager?

        return Preference.PreferenceGroup(
            title = stringResource(R.string.background_activity),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.disable_battery_optimization),
                        subtitle = stringResource(R.string.disable_if_issues_with_updating),
                        enabled = powerManager != null,
                        onClick = {
                            val packageName: String = context.packageName
                            if (!powerManager!!.isIgnoringBatteryOptimizations(packageName)) {
                                val intent =
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                context.startActivity(intent)
                            } else {
                                context.toast(R.string.battery_optimization_disabled)
                            }
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.dont_kill_my_app),
                        subtitle = stringResource(R.string.about_dont_kill_my_app),
                        onClick = { context.openInBrowser(DONT_KILL_MY_APP_URL) },
                    ),
                ),
        )
    }

    @Composable
    fun networkGroup(
        context: Context,
        clearNetworkCookies: () -> Unit,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.network),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.clear_cookies),
                        onClick = { clearNetworkCookies() },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.pref_clear_webview_data),
                        onClick = {
                            try {
                                val webview = WebView(context)
                                webview.setDefaultSettings()
                                webview.clearCache(true)
                                webview.clearFormData()
                                webview.clearHistory()
                                webview.clearSslPreferences()
                                WebStorage.getInstance().deleteAllData()
                                context.applicationInfo.dataDir?.let {
                                    File("$it/app_webview/").deleteRecursively()
                                }
                                context.toast(R.string.webview_data_deleted)
                            } catch (e: Throwable) {
                                TimberKt.e(e) { "Error clearing webview data" }
                                context.toast(R.string.cache_delete_error)
                            }
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = networkPreferences.dohProvider(),
                        title = stringResource(R.string.doh),
                        entries =
                            mapOf(
                                    -1 to stringResource(R.string.disabled),
                                    PREF_DOH_CLOUDFLARE to stringResource(R.string.cloudflare),
                                    PREF_DOH_GOOGLE to stringResource(R.string.google),
                                    PREF_DOH_ADGUARD to stringResource(R.string.ad_guard),
                                    PREF_DOH_QUAD9 to stringResource(R.string.quad9),
                                    PREF_DOH_ALIDNS to stringResource(R.string.aliDNS),
                                    PREF_DOH_DNSPOD to stringResource(R.string.dnsPod),
                                    PREF_DOH_360 to stringResource(R.string.dns_360),
                                    PREF_DOH_QUAD101 to stringResource(R.string.quad_101),
                                    PREF_DOH_MULLVAD to stringResource(R.string.mullvad),
                                    PREF_DOH_CONTROLD to stringResource(R.string.control_d),
                                    PREF_DOH_NJALLA to stringResource(R.string.njalla),
                                    PREF_DOH_SHECAN to stringResource(R.string.shecan),
                                )
                                .toImmutableMap(),
                        onValueChanged = {
                            context.toast(R.string.requires_app_restart)
                            true
                        },
                    ),
                ),
        )
    }

    @Composable
    fun dataGroup(
        cleanupDownloads: (Boolean, Boolean) -> Unit,
        clearDatabase: (Boolean) -> Unit,
        reindexDownloads: () -> Unit,
    ): Preference.PreferenceGroup {

        var showCleanDownloadsDialog by rememberSaveable { mutableStateOf(false) }
        var showClearDatabaseDialog by rememberSaveable { mutableStateOf(false) }

        if (showCleanDownloadsDialog) {
            CleanDownloadsDialog(
                onDismiss = { showCleanDownloadsDialog = false },
                onConfirm = cleanupDownloads,
            )
        }

        if (showClearDatabaseDialog) {
            ClearDatabaseDialog(
                onDismiss = { showClearDatabaseDialog = false },
                onConfirm = clearDatabase,
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.data_management),
            preferenceItems =
                persistentListOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.reindex_downloads),
                        subtitle = stringResource(R.string.reindex_downloads_summary),
                        onClick = reindexDownloads,
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.clean_up_downloaded_chapters),
                        subtitle = stringResource(R.string.delete_unused_chapters),
                        onClick = { showCleanDownloadsDialog = true },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.clear_database),
                        subtitle = stringResource(R.string.clear_database_summary),
                        onClick = { showClearDatabaseDialog = true },
                    ),
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): PersistentList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    title = stringResource(R.string.send_crash_report),
                    subtitle = stringResource(R.string.helps_fix_bugs),
                ),
                SearchTerm(
                    title = stringResource(R.string.dump_crash_logs),
                    subtitle = stringResource(R.string.saves_error_logs),
                ),
                SearchTerm(
                    title = stringResource(R.string.verbose_logging),
                    subtitle = stringResource(R.string.verbose_logging_summary),
                ),
                SearchTerm(
                    title = stringResource(R.string.disable_battery_optimization),
                    subtitle = stringResource(R.string.disable_if_issues_with_updating),
                    group = stringResource(R.string.background_activity),
                ),
                SearchTerm(
                    title = stringResource(R.string.dont_kill_my_app),
                    subtitle = stringResource(R.string.about_dont_kill_my_app),
                    group = stringResource(R.string.background_activity),
                ),
                SearchTerm(
                    title = stringResource(R.string.clear_cookies),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.pref_clear_webview_data),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.doh),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.reindex_downloads),
                    subtitle = stringResource(R.string.reindex_downloads_summary),
                    group = stringResource(R.string.data_management),
                ),
                SearchTerm(
                    title = stringResource(R.string.clean_up_downloaded_chapters),
                    subtitle = stringResource(R.string.delete_unused_chapters),
                    group = stringResource(R.string.data_management),
                ),
                SearchTerm(
                    title = stringResource(R.string.clear_database),
                    subtitle = stringResource(R.string.clear_database_summary),
                    group = stringResource(R.string.data_management),
                ),
            )
        }
    }
}
