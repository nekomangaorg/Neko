# Sentinel Journal

## 2024-05-15 - Missing SOURCE_KEY in WebViewActivity
**Vulnerability:** WebViewActivity can crash due to an explicit crash if `SOURCE_KEY` is null or if `intent.extras` is null when it is launched via Intent that doesn't include it. NullPointerExceptions can be used for DoS attacks locally by malicious apps if an activity is exported, though `WebViewActivity` isn't necessarily exported, intent extra crashes are a common pattern of unhandled intent data causing app instability.
**Learning:** In Android, `intent.extras` might be null, and accessing keys blindly can cause unhandled exceptions. In `WebViewActivity.kt` `intent.extras!!.getString(URL_KEY) ?: return` and `intent.extras!!.getLong(SOURCE_KEY)` throws NPE if extras is null, or if it isn't set.
**Prevention:** Always use safe access `intent.extras?.getString` and handle missing keys securely without throwing NPE.
