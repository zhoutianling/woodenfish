import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aab.resguard)
}

apply(plugin = "stringfog")

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    mode = StringFogMode.bytes
}

configure<AabResGuardExtension> {
    enableObfuscate = true
    obfuscatedBundleFileName = "woodenfish-obfuscated.aab"
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("../doc/woodenfish.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }
    namespace = "com.zero.woodenfish"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.zero.woodenfish"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.material)
    implementation(libs.stringfog.xor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
