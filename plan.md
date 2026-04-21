1. **Add string resources**
   - Add new strings `supported_links` and `supported_links_summary` to `constants/src/main/res/values/strings.xml`.

2. **Add Preference to AdvancedSettingsScreen**
   - In `app/src/main/java/org/nekomanga/presentation/screens/settings/screens/AdvancedSettingsScreen.kt`, add a new `Preference.PreferenceItem.TextPreference` under the `backgroundActivityGroup` since that handles system settings (or just put it at the bottom of the group or its own).
   - Use `Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS` if API level >= 31, otherwise fallback to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

3. **Complete pre commit steps**
   - 3. Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.

4. **Submit**
