plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    // Kotlin is built into AGP 9 (no org.jetbrains.kotlin.android plugin needed).
    // These plugin versions must match the KGP version AGP bundles: 2.2.10.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
