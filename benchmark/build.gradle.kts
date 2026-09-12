// Macrobenchmark + Baseline Profile module.
//
// Runs against the :app module's `benchmark` build type (release-like,
// profileable). Flavor dimension mirrors :app so variants line up.
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
    flavorDimensions += "distribution"
    productFlavors {
        create("free") { dimension = "distribution" }
        create("play") { dimension = "distribution" }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
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
