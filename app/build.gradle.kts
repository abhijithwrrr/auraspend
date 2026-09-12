plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
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
        // Release-like build for Macrobenchmark / baseline profiles.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    // Unit tests exercise JVM-only logic; unmocked android.framework calls (e.g. Log) no-op.
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric + Roborazzi render real resources (fonts, colors) on the JVM.
        unitTests.isIncludeAndroidResources = true
        // Screenshot tests record directly into the committed baseline directory;
        // CI fails if regenerating changes any tracked PNG (git diff).
        unitTests.all {
            it.systemProperty("roborazzi.test.record", "true")
        }
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
    // Compose — versions are aligned by the BOM (Material 3 1.4.0, Compose UI 1.12.1).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // androidx core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Foreground/background process detection for background classification.
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    // Google Sign-In & Drive
    implementation(libs.play.services.auth)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.guava)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Installs baseline profiles at first boot when present (startup performance).
    implementation(libs.androidx.profileinstaller)

    // On-device LLM runtime (llama.cpp) and model downloader
    implementation(project(":llama"))
    implementation(libs.okhttp)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Room PagingSource integration + collectAsLazyPagingItems (Activity list).
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.compose)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // JVM screenshot tests (Robolectric + Roborazzi) for visual regressions.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose previews / inspection in debug builds only.
    debugImplementation(libs.androidx.compose.ui.tooling)
}
