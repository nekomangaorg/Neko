## 2026-04-24 - [Extracting BuildTime to prevent cache invalidation] **Learning:** [Calling LocalDateTime.now() directly in gradle script breaks configuration cache and local build caches because Gradle thinks the input is constantly changing] **Action:** [Conditionally use a static ISO-8601 placeholder for local builds and only evaluate the actual timestamp on CI using providers.environmentVariable("CI")]

## 2026-06-29 - [Modernizing Configuration Cache & JVM Settings]
**Learning:** [Using the legacy `org.gradle.unsafe.configuration-cache` property is deprecated. Renaming it to `org.gradle.configuration-cache` and optimizing Gradle's JVM args with G1GC (`-XX:+UseG1GC`) improves garbage collection efficiency and build caching stability.]
**Action:** [Prefer the stable `org.gradle.configuration-cache` property and supply optimized GC/JVM daemon parameters in gradle.properties.]
