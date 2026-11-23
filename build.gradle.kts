// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    // Declara el plugin de Firebase para que el módulo app pueda aplicarlo solo si posee google-services.json.
    alias(libs.plugins.google.services) apply false
}
