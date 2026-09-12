// Vendored llama.cpp Android runtime library (official "llama.android" module).
// Source: https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android
// Cleaned to match AuraSpend's toolchain (AGP 8.9.1 / Kotlin 2.0.0 / NDK 29).
plugins {
    id("com.android.library")
}

android {
    namespace = "com.arm.aichat"
    compileSdk = 36

    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 30

        consumerProguardFiles("consumer-rules.pro")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DBUILD_SHARED_LIBS=ON"
                arguments += "-DLLAMA_BUILD_APP=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DLLAMA_OPENSSL=OFF"
                arguments += "-DGGML_NATIVE=OFF"
                arguments += "-DGGML_BACKEND_DL=ON"
                arguments += "-DGGML_CPU_ALL_VARIANTS=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
            }
        }
    }
    externalNativeBuild {
        cmake {
            // NOTE: native compilation requires CMake >= 3.31.6 and NDK 29.
            // The Kotlin layer (used by unit tests) does NOT require a native build.
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Built-in Kotlin (AGP 9): jvmTarget defaults to compileOptions.targetCompatibility.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.core:core-ktx:1.13.1")

    testImplementation("junit:junit:4.13.2")
}
