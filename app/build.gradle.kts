import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

// Release signing credentials live in secrets.properties (gitignored). The file is optional:
// without it the release build still configures, but is explicitly reported as UNSIGNED so a
// green "assembleFreeRelease" can never be mistaken for a shippable artifact again.
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { stream -> load(stream) }
    }
}
fun secret(key: String): String? = secrets.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
val releaseStorePath = secret("KEYSTORE_PATH")
val hasReleaseSigning = releaseStorePath != null &&
    secret("KEYSTORE_PASSWORD") != null &&
    secret("KEY_ALIAS") != null &&
    secret("KEY_PASSWORD") != null

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

        // Native ABI filter. ONNX Runtime's prebuilt .so is ~95% of the app's
        // footprint, so the ABI list is the single biggest size lever there is.
        // Measured 2026-09-29, native payload per ABI:
        //
        //   arm64-v8a     17.5 MB   real consumer hardware
        //   armeabi-v7a   12.7 MB   32-bit low-end hardware still ships
        //   x86           20.6 MB   dropped — no device or current emulator
        //   x86_64        21.0 MB   dropped — no Chromebooks, no emulators
        //
        // Play's 64-bit requirement is met by arm64-v8a. The cost is 21 MB of
        // native payload, which takes the release AAB from 35.4 MB to 20.5 MB.
        //
        // **ChromeOS is not a supported form factor.** Dropping x86_64 makes the
        // app uninstallable on Chromebooks. That is deliberate, not an
        // oversight — see the `ChromeOsAbiSupport` suppression in `lint` below,
        // which is the place to change your mind.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
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

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = secret("KEYSTORE_PASSWORD")
                keyAlias = secret("KEY_ALIAS")
                keyPassword = secret("KEY_PASSWORD")
            }
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
            // Previously this type had no signingConfig at all, so `assembleFreeRelease`
            // produced an uninstallable APK. Now it is signed whenever credentials exist.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "AuraSpend: no release keystore in secrets.properties " +
                        "(KEYSTORE_PATH / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD) — " +
                        "release builds will be UNSIGNED."
                )
                null
            }
        }
        // Release-like build for Macrobenchmark / baseline profiles.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    // Static analysis. There was no lint configuration at all, which is how the
    // scoped-storage CSV crash and the unguarded enum valueOf calls slipped through
    // a "green" build. The 100+ pre-existing issues are pinned in
    // app/lint-baseline.xml; anything new fails the build.
    // Prints the accuracy floor for the golden corpus (regex only, no on-device AI).
    // Any runtime swap is judged against this number, so it has to be cheap to read.
    tasks.register("classificationBaseline") {
        group = "verification"
        description = "Run the golden SMS corpus through the regex path and print per-field accuracy."
        dependsOn("testFreeDebugUnitTest")
        doLast {
            logger.lifecycle("See the REGEX-ONLY BASELINE block in the test output for :app:testFreeDebugUnitTest")
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
        checkReleaseBuilds = true
        explainIssues = true
        xmlReport = true
        htmlReport = true
        baseline = file("lint-baseline.xml")

        // AuraSpend does not support ChromeOS, by decision rather than oversight.
        // Chromebooks run Android apps on x86_64, so removing that ABI from
        // `abiFilters` (which saves 21 MB of ONNX Runtime native payload and
        // takes the release AAB from 35.4 MB to 20.5 MB) also makes the app
        // uninstallable there. Lint flags that combination by default; the
        // check is disabled deliberately rather than worked around.
        //
        // To change this: add "x86_64" back to `abiFilters` and delete this line.
        // Do not suppress any other check to recover the size.
        disable += "ChromeOsAbiSupport"
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
    // viewModel() + ViewModelStoreOwner-scoped factories (survives rotation).
    implementation(libs.androidx.lifecycle.viewmodel.compose)
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

    // On-device inference. The encoder runs on ONNX Runtime Mobile; the model
    // itself is downloaded separately (22 MB, SHA-256 verified) rather than
    // shipped in the APK, so users who never opt in pay nothing for it.
    implementation(libs.onnxruntime.android)
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
