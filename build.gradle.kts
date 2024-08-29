// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.ksp) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}


buildscript {
    dependencies {
        // This is generally unnecessary if you're using the `plugins` block correctly.
        // If you still need this, ensure it's properly aligned with the version you're using.
        // classpath(libs.com.google.devtools.ksp.gradle.plugin)
    }
}
