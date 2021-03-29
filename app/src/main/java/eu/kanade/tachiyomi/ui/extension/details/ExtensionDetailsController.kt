package eu.kanade.tachiyomi.ui.extension.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogController
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogController
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogController
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.EmptyPreferenceDataStore
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import eu.kanade.tachiyomi.data.preference.minusAssign
import eu.kanade.tachiyomi.data.preference.plusAssign
import eu.kanade.tachiyomi.databinding.ExtensionDetailControllerBinding
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getPreferenceKey
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.setting.DSL
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.switchPreference
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.openInBrowser
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.widget.preference.ListMatPreference
import kotlinx.android.synthetic.main.extension_detail_controller.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@SuppressLint("RestrictedApi")
class ExtensionDetailsController(bundle: Bundle? = null) :
    NucleusController<ExtensionDetailControllerBinding, ExtensionDetailsPresenter>(bundle),
    PreferenceManager.OnDisplayPreferenceDialogListener,
    DialogPreference.TargetFragment {

    private var lastOpenPreferencePosition: Int? = null

    private var preferenceScreen: PreferenceScreen? = null

    private val preferences: PreferencesHelper = Injekt.get()

    private val viewScope = MainScope()
    init {
        setHasOptionsMenu(true)
    }

    constructor(pkgName: String) : this(
        Bundle().apply {
            putString(PKGNAME_KEY, pkgName)
        }
    )

    override fun createBinding(inflater: LayoutInflater) =
        ExtensionDetailControllerBinding.inflate(inflater.cloneInContext(getPreferenceThemeContext()))

    override fun createPresenter(): ExtensionDetailsPresenter {
        return ExtensionDetailsPresenter(args.getString(PKGNAME_KEY)!!)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.extension_info)
    }

    @SuppressLint("PrivateResource")
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        scrollViewWith(extension_prefs_recycler, padBottom = true)

        val extension = presenter.extension ?: return
        val context = view.context

        val themedContext by lazy { getPreferenceThemeContext() }
        val manager = PreferenceManager(themedContext)
        manager.preferenceDataStore = EmptyPreferenceDataStore()
        manager.onDisplayPreferenceDialogListener = this
        val screen = manager.createPreferenceScreen(themedContext)
        preferenceScreen = screen

        val multiSource = extension.sources.size > 1
        val isMultiLangSingleSource = multiSource && extension.sources.map { it.name }.distinct().size == 1
        val langauges = preferences.enabledLanguages().get()

        for (source in extension.sources.sortedByDescending { it.isLangEnabled(langauges) }) {
            if (source is ConfigurableSource) {
                addPreferencesForSource(screen, source, multiSource, isMultiLangSingleSource)
            }
        }

        manager.setPreferences(screen)

        extension_prefs_recycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
        val concatAdapterConfig = ConcatAdapter.Config.Builder()
            .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
            .build()
        screen.setShouldUseGeneratedIds(true)
        val extHeaderAdapter = ExtensionDetailsHeaderAdapter(presenter)
        extHeaderAdapter.setHasStableIds(true)
        extension_prefs_recycler.adapter = ConcatAdapter(
            concatAdapterConfig,
            extHeaderAdapter,
            PreferenceGroupAdapter(screen)
        )
        extension_prefs_recycler.addItemDecoration(ExtensionSettingsDividerItemDecoration(context))
    }

    override fun onDestroyView(view: View) {
        preferenceScreen = null
        super.onDestroyView(view)
    }

    fun onExtensionUninstalled() {
        router.popCurrentController()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        lastOpenPreferencePosition?.let { outState.putInt(LASTOPENPREFERENCE_KEY, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastOpenPreferencePosition = savedInstanceState.get(LASTOPENPREFERENCE_KEY) as? Int
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extension_details, menu)

        menu.findItem(R.id.action_history).isVisible = presenter.extension?.isUnofficial == false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_history -> openCommitHistory()
            R.id.action_app_info -> openInSettings()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCommitHistory() {
        val pkgName = presenter.extension!!.pkgName.substringAfter("eu.kanade.tachiyomi.extension.")
        val pkgFactory = presenter.extension!!.pkgFactory
        val url = when {
            !pkgFactory.isNullOrEmpty() -> "$URL_EXTENSION_COMMITS/multisrc/src/main/java/eu/kanade/tachiyomi/multisrc/$pkgFactory"
            else -> "$URL_EXTENSION_COMMITS/src/${pkgName.replace(".", "/")}"
        }
        openInBrowser(url)
    }

    private fun openInSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", presenter.pkgName, null)
        }
        startActivity(intent)
    }

    private fun addPreferencesForSource(screen: PreferenceScreen, source: Source, isMultiSource: Boolean, isMultiLangSingleSource: Boolean) {
        val context = screen.context

        // TODO
        val dataStore = SharedPreferencesDataStore(
            context.getSharedPreferences("source_${source.id}", Context.MODE_PRIVATE)
        )

        if (source is ConfigurableSource) {
            val prefs = mutableListOf<Preference>()
            val block: (@DSL SwitchPreferenceCompat).() -> Unit = {
                key = source.getPreferenceKey()
                title = when {
                    isMultiSource && !isMultiLangSingleSource -> source.toString()
                    else -> LocaleHelper.getSourceDisplayName(source.lang, context)
                }
                isPersistent = false
                isChecked = source.isEnabled()

                onChange { newValue ->
                    if (source.isLangEnabled()) {
                        val checked = newValue as Boolean
                        toggleSource(source, checked)
                        prefs.forEach { it.isVisible = checked }
                        true
                    } else {
                        coordinator.snack(context.getString(R.string._must_be_enabled_first, title), Snackbar.LENGTH_LONG) {
                            setAction(R.string.enable) {
                                preferences.enabledLanguages() += source.lang
                                isChecked = true
                                toggleSource(source, true)
                                prefs.forEach { it.isVisible = true }
                            }
                        }
                        false
                    }
                }

                // React to enable/disable all changes
                preferences.hiddenSources().asFlow()
                    .onEach {
                        val enabled = source.isEnabled()
                        isChecked = enabled
                    }
                    .launchIn(viewScope)
            }

            val newScreen = screen.preferenceManager.createPreferenceScreen(context)
            screen.switchPreference(block)
            source.setupPreferenceScreen(newScreen)

            // Reparent the preferences
            while (newScreen.preferenceCount != 0) {
                val pref = newScreen.getPreference(0)
                pref.isIconSpaceReserved = true
                pref.preferenceDataStore = dataStore
                pref.fragment = "source_${source.id}"
                pref.order = Int.MAX_VALUE
                pref.isVisible = source.isEnabled()
                prefs.add(pref)
                newScreen.removePreference(pref)
                screen.addPreference(pref)
            }
        }
    }

    private fun toggleSource(source: Source, enable: Boolean) {
        if (enable) {
            preferences.hiddenSources() -= source.id.toString()
        } else {
            preferences.hiddenSources() += source.id.toString()
        }
    }

    private fun getPreferenceThemeContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (!isAttached) return

        val screen = preference.parent!!

        lastOpenPreferencePosition = (0 until screen.preferenceCount).indexOfFirst {
            screen.getPreference(it) === preference
        }

        if (preference is ListPreference) {
            ListMatPreference(activity, preference.context).apply {
                key = preference.key
                sharedPref = preference.fragment
                otherPref = preference
                preferenceDataStore = preference.preferenceDataStore
                entries = preference.entries.mapNotNull { it.toString() }
                entryValues = preference.entryValues.mapNotNull { it.toString() }
            }.dialog().show()
            return
        }

        val f = when (preference) {
            is EditTextPreference ->
                EditTextPreferenceDialogController
                    .newInstance(preference.getKey())
            is ListPreference ->
                ListPreferenceDialogController
                    .newInstance(preference.getKey())
            is MultiSelectListPreference ->
                MultiSelectListPreferenceDialogController
                    .newInstance(preference.getKey())
            else -> throw IllegalArgumentException(
                "Tried to display dialog for unknown " +
                    "preference type. Did you forget to override onDisplayPreferenceDialog()?"
            )
        }
        f.targetController = this
        f.showDialog(router)
    }

    private fun Source.isEnabled(): Boolean {
        return id.toString() !in preferences.hiddenSources().get() && isLangEnabled()
    }

    private fun Source.isLangEnabled(langs: Set<String>? = null): Boolean {
        return (lang in langs ?: preferences.enabledLanguages().get())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Preference> findPreference(key: CharSequence): T? {
        // We track [lastOpenPreferencePosition] when displaying the dialog
        // [key] isn't useful since there may be duplicates
        return preferenceScreen!!.getPreference(lastOpenPreferencePosition!!) as T
    }

    private companion object {
        const val PKGNAME_KEY = "pkg_name"
        const val LASTOPENPREFERENCE_KEY = "last_open_preference"
        private const val URL_EXTENSION_COMMITS =
            "https://github.com/tachiyomiorg/tachiyomi-extensions/commits/master"
    }
}
