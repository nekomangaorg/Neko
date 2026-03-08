# Sentinel Journal

## 2024-05-15 - Missing SOURCE_KEY in WebViewActivity
**Vulnerability:** WebViewActivity can crash due to an explicit crash if `SOURCE_KEY` is null or if `intent.extras` is null when it is launched via Intent that doesn't include it. NullPointerExceptions can be used for DoS attacks locally by malicious apps if an activity is exported, though `WebViewActivity` isn't necessarily exported, intent extra crashes are a common pattern of unhandled intent data causing app instability.
**Learning:** In Android, `intent.extras` can be null. Accessing it with the non-null assertion operator (`!!`), as in `intent.extras!!`, will cause a `NullPointerException` if it is indeed null. In `WebViewActivity.kt`, this was the case for `intent.extras!!.getString(URL_KEY)` and `intent.extras!!.getLong(SOURCE_KEY)`, leading to a crash when the Activity was started with an Intent without extras.
**Prevention:** Always use safe access `intent.extras?.getString` and handle missing keys securely without throwing NPE.
