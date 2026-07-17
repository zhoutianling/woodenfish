// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.github.mobcoding.StringFog:gradle-plugin:5.3.3")
        classpath("com.github.mobcoding.StringFog:xor:5.3.3")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
