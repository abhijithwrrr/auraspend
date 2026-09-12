plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.awbuilds.auraspend"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.awbuilds.auraspend"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("free") {
            dimension = "distribution"
            versionNameSuffix = "-free"
        }
        create("play") {
            dimension = "distribution"
            versionNameSuffix = "-play"
        }
    }

    buildTypes {
        debug {
            // Keep debug builds fast: no shrinking, but still use the same dex pipeline.
            isPseudoLocalesEnabled = false
        }
        release {
            // Full R8: shrink + optimize + obfuscate + resource shrinking.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Unit tests exercise JVM-only logic; unmocked android.framework calls (e.g. Log) no-op.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Built-in Kotlin (AGP 9): jvmTarget defaults to compileOptions.targetCompatibility.
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // androidx
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    // Foreground/background process detection for background classification.
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.2")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Google Sign-In & Drive
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
    implementation("com.google.api-client:google-api-client:2.9.0")
    implementation("com.google.api-client:google-api-client-android:2.9.0")
    implementation("com.google.http-client:google-http-client-gson:1.44.2")
    implementation("com.google.guava:guava:33.0.0-android")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Installs baseline profiles at first boot when present (startup performance).
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // On-device LLM runtime (llama.cpp) and model downloader
    implementation(project(":llama"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
