object Libs {

    object Android {
        const val annotations = "androidx.annotation:annotation:1.2.0"
        const val appCompat = "androidx.appcompat:appcompat:1.3.0-rc01"
        const val biometric = "androidx.biometric:biometric:1.1.0"
        const val browser = "androidx.browser:browser:1.3.0"
        const val cardView = "androidx.cardview:cardview:1.0.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.4"
        const val coreKtx = "androidx.core:core-ktx:1.5.0-rc01"
        const val lifecycleCommonJava8 = "androidx.lifecycle:lifecycle-common-java8:2.2.0"
        const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
        const val lifecycleRuntimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
        const val material = "com.google.android.material:material:1.4.0-beta01"
        const val multiDex = "androidx.multidex:multidex:2.0.1"
        const val palette = "androidx.palette:palette:1.0.0"
        const val preference = "androidx.preference:preference:${Versions.androidPreferences}"
        const val recyclerView = "androidx.recyclerview:recyclerview:1.2.0"
        const val workManager = "androidx.work:work-runtime:${Versions.androidWorkManager}"
        const val workManagerKtx = "androidx.work:work-runtime-ktx:${Versions.androidWorkManager}"
        const val dataBinding = "com.android.databinding:compiler:${Versions.dataBinding}"
    }

    object Database {
        const val requerySqlite = "io.requery:sqlite-android:3.31.0"
        const val sqlite = "androidx.sqlite:sqlite:2.1.0"
        const val storioCommon = "com.github.inorichi.storio:storio-common:8be19de@aar"
        const val storioSqlite = "com.github.inorichi.storio:storio-sqlite:8be19de@aar"
    }

    object Disk {
        const val lrucache = "com.jakewharton:disklrucache:2.0.2"
        const val unifile = "com.github.tachiyomiorg:unifile:17bec43"
    }

    object Google {
        const val firebaseAnayltics = "com.google.firebase:firebase-core:18.0.3"
        const val firebaseCore = "com.google.firebase:firebase-core:18.0.3"
        const val firebaseCrashltyics = "com.google.firebase:firebase-crashlytics:${Versions.firebaseCrashlytics}"
        const val playServices = "com.google.android.gms:play-services-gcm:${Versions.googlePlayServices}"
    }

    object Hyperion {
        private const val version = "0.9.27"
        const val attr = "com.willowtreeapps.hyperion:hyperion-attr:$version"
        const val buildConfig = "com.willowtreeapps.hyperion:hyperion-build-config:$version"
        const val core = "com.willowtreeapps.hyperion:hyperion-core:$version"
        const val crash = "com.willowtreeapps.hyperion:hyperion-crash:$version"
        const val disk = "com.willowtreeapps.hyperion:hyperion-disk:$version"
        const val geigerCounter = "com.willowtreeapps.hyperion:hyperion-geiger-counter:$version"
        const val measurement = "com.willowtreeapps.hyperion:hyperion-measurement:$version"
        const val phoenix = "com.willowtreeapps.hyperion:hyperion-phoenix:$version"
        const val recorder = "com.willowtreeapps.hyperion:hyperion-recorder:$version"
        const val sharedPreferences = "com.willowtreeapps.hyperion:hyperion-shared-preferences:$version"
        const val timber = "com.willowtreeapps.hyperion:hyperion-timber:$version"
    }

    object Image {
        private const val version = "1.2.1"
        const val coil = "io.coil-kt:coil:$version"
        const val coilGif = "io.coil-kt:coil-gif:$version"
        const val coilSvg = "io.coil-kt:coil-svg:$version"
    }

    object IO {
        const val gson = "com.google.code.gson:gson:2.8.6"
        const val kotson = "com.github.salomonbrys.kotson:kotson:2.5.0"
        const val moshi = "com.squareup.moshi:moshi:1.9.3"
        const val okio = "com.squareup.okio:okio:2.10.0"
    }

    object Kotlin {
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.2"
        const val flowPreferences = "com.github.tfcporciuncula:flow-preferences:1.3.4"
        const val stdLib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
        const val reflection = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
        val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0"
        const val serializationProtobuf = "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.1.0"
    }

    object Navigation {
        private const val version = "3.0.0"
        const val conductor = "com.bluelinelabs:conductor:$version"
        const val conductorSupportPreferences = "com.github.jays2kings:conductor-support-preference:3.0.0"

        const val nucleus = "info.android15.nucleus:nucleus:3.0.0"
        const val nucleusSupport = "info.android15.nucleus:nucleus-support-v7:3.0.0"
    }

    object Network {
        const val chucker = "com.github.ChuckerTeam.Chucker:library:3.2.0"
        const val chuckerNoOp = "com.github.ChuckerTeam.Chucker:library-no-op:3.2.0"
        const val okhttp = "com.squareup.okhttp3:okhttp:4.9.1"
        const val okhttpDns = "com.squareup.okhttp3:okhttp-dnsoverhttps:4.9.1"
        const val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:4.9.1"
        const val retrofit = "com.squareup.retrofit2:retrofit:2.7.2"
        const val retrofitGsonConverter = "com.squareup.retrofit2:converter-gson:2.7.2"
        const val retrofitKotlinxConverter = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.7.0"
    }

    object Parsing {
        const val jsoup = "org.jsoup:jsoup:1.13.1"
    }

    object Rx {
        const val android = "io.reactivex:rxandroid:1.2.1"
        private const val rxbinding = "1.0.1"
        const val bindingAppcompat = "com.jakewharton.rxbinding:rxbinding-appcompat-v7-kotlin:$rxbinding"
        const val bindingKotlin = "com.jakewharton.rxbinding:rxbinding-kotlin:$rxbinding"
        const val bindingRecycler = "com.jakewharton.rxbinding:rxbinding-recyclerview-v7-kotlin:$rxbinding"
        const val bindingSupport = "com.jakewharton.rxbinding:rxbinding-support-v4-kotlin:$rxbinding"
        const val java = "io.reactivex:rxjava:1.3.8"
        const val network = "com.github.pwittchen:reactivenetwork:0.13.0"
        const val preferences = "com.f2prateek.rx.preferences:rx-preferences:1.0.2"
        const val relay = "com.jakewharton.rxrelay:rxrelay:1.2.0"
    }

    // Tests
    object Test {
        const val assertJCore = "org.assertj:assertj-core:${Versions.assertJ}"
        const val junit4 = "junit:junit:${Versions.junit}"
        const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
        const val roboElectric = "org.robolectric:robolectric:${Versions.roboElectric}"
        const val roboElectricMultidex = "org.robolectric:shadows-multidex:${Versions.roboElectric}"
        const val roboElectricShadowPlayServices = "org.robolectric:shadows-play-services:${Versions.roboElectric}"
    }

    object UI {
        const val androidTagGroup = "com.github.kizitonwose:AndroidTagGroup:1.6.0"
        const val directionalPageView = "com.github.carlosesco:DirectionalViewPager:${Versions.directionalViewPager}"
        const val fastAdapter = "com.mikepenz:fastadapter:5.4.1"
        const val fastAdapterBinding = "com.mikepenz:fastadapter-extensions-binding:5.4.1"
        const val filePicker = "com.nononsenseapps:filepicker:2.4.2"
        const val flexibleAdapter = "eu.davidea:flexible-adapter:5.1.0"
        const val flexibleAdapterUi = "eu.davidea:flexible-adapter-ui:1.0.0"
        const val loadingButton = "br.com.simplepass:loading-button-android:2.2.0"
        const val materalDesignDimens = "com.dmitrymalkovich.android:material-design-dimens:1.4"
        const val materialDialogsCore = "com.afollestad.material-dialogs:core:3.1.1"
        const val materialDialogsInput = "com.afollestad.material-dialogs:input:3.1.1"
        const val materialDateTime = "com.afollestad.material-dialogs:datetime:3.1.1"

        const val numberSlidingPicker = "com.github.sephiroth74:NumberSlidingPicker:${Versions.numberSlidingPicker}"
        const val photoView = "com.github.chrisbanes:PhotoView:${Versions.photoView}"
        const val subsamplingScaleImageView = "com.github.jays2kings:subsampling-scale-image-view:dfd3e43"
        const val systemUiHelper = "me.zhanghai.android.systemuihelper:library:${Versions.systemUiHelper}"
        const val tapTargetView = "com.getkeepsafe.taptargetview:taptargetview:1.13.0"
        const val viewStatePager = "com.nightlynexus.viewstatepageradapter:viewstatepageradapter:1.1.0"
        const val viewToolTip = "com.github.florent37:viewtooltip:${Versions.viewToolTip}"
    }

    object Util {
        const val aboutLibraries = "com.mikepenz:aboutlibraries:8.3.0"
        const val changelog = "com.github.gabrielemariotti.changeloglib:changelog:2.1.0"
        const val injekt = "com.github.inorichi.injekt:injekt-core:65b0440"
        const val stringSimilarity = "info.debatty:java-string-similarity:2.0.0"
        const val tokenBucket = "org.isomorphism:token-bucket:1.7"
        const val versionCompare = "com.g00fy2:versioncompare:1.3.4"
        const val listenableFutureConflictResolve = "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"
        const val xlog = "com.elvishew:xlog:1.8.0"
    }
}

object Versions {
    const val androidGradlePlugin = "4.2.1"
    const val androidPreferences = "1.1.1"
    const val androidWorkManager = "2.4.0"
    const val dataBinding = "3.1.4"
    const val assertJ = "3.12.2"
    const val changelog = "2.1.0"
    const val coil = "1.1.1"
    const val conductor = "2.1.5"
    const val directionalViewPager = "a844dbca0a"
    const val firebaseCrashlytics = "17.2.1"
    const val googlePlayServices = "17.0.0"
    const val googleServices = "4.3.3"
    const val gradleVersions = "0.29.0"
    const val junit = "4.13"
    const val kotlin = "1.4.32"
    const val loadingButton = "2.2.0"
    const val mockito = "1.10.19"
    const val moshi = "1.9.3"
    const val nucleus = "3.0.0"
    const val numberSlidingPicker = "1.0.3"
    const val photoView = "2.3.0"
    const val roboElectric = "3.1.4"
    const val systemUiHelper = "1.0.0"

    //const val unifile = "e9ee588"
    const val viewToolTip = "1.2.2"
    const val xlog = "1.8.0"
    const val kotlinter = "3.4.0"
}

