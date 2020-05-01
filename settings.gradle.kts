pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                useVersion("1.3.70")
            } else if (requested.id.id.equals("com.android.application")) {
                useModule("com.android.tools.build:gradle:3.6.0")
            } else if (requested.id.id.equals("com.google.gms.google-services")) {
                useModule("com.google.gms:google-services:4.3.3")
            } else if (requested.id.id.equals("com.google.gms.google-services")) {
                useModule("com.google.gms:google-services:4.3.3")
            } else if (requested.id.id.equals("com.google.firebase.crashlytics")) {
                useModule("com.google.firebase:firebase-crashlytics-gradle:2.0.0-beta03")
            }else if(requested.id.id.equals("com.google.android.gms.oss-licenses-plugin")){
                useModule("com.google.android.gms:oss-licenses-plugin:0.10.2")
            }

        }
    }
}

include(":app")
