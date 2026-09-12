plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Kotlin is built into AGP 9 (no org.jetbrains.kotlin.android plugin needed).
    // These plugin versions must match the KGP version AGP bundles: 2.2.10.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
