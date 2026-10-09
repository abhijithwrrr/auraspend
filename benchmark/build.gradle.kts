// Macrobenchmark + Baseline Profile module.
//
// Runs against the :app module's `release` build type: the app deliberately
// has no benchmark build type any more (ADR 0010 keeps :app to exactly four
// variants — dev/prod × debug/release), so this module's `benchmark` build type
// declares matchingFallbacks and links against the app's release variant. The
// release build is profileable and not debuggable, which is what macrobenchmark
// needs — but it is signed with the *release* keystore, so running benchmarks
// locally requires secrets.properties (see secrets.properties.example).
//
// Flavor dimension mirrors :app so variants line up.
plugins {
    id("com.android.test")
}

android {
    namespace = "com.awbuilds.auraspend.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Match the app's flavor dimension.
    flavorDimensions += "env"
    productFlavors {
        create("dev") { dimension = "env" }
        create("prod") { dimension = "env" }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            // The app has no benchmark type; link its release build instead.
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
