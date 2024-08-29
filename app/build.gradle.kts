plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.ksp) // Ensure this is correctly linked in libs.versions.toml
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id ("kotlin-android")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.camera"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.camera"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    viewBinding{
        enable = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Ensure this is compatible with Kotlin 1.9.0
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.activity.compose.v190)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle.v140rc01)
    implementation(libs.androidx.camera.view.v100alpha31)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui.graphics)
    implementation(libs.firebase.inappmessaging.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.glide)
    debugImplementation(libs.ui.tooling)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    implementation(libs.material)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.recyclerview)
    implementation(libs.guava)
    implementation(libs.kotlinx.coroutines.android.v160)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)  // Use KSP for Room compiler
    implementation(libs.room.ktx)  // Add coroutine support for Room
    implementation(libs.ucrop)
    implementation(libs.androidx.core.splashscreen)
    implementation (platform(libs.firebase.bom))
    implementation (libs.firebase.auth.ktx)
    implementation (libs.firebase.database.ktx)
    implementation (libs.firebase.storage.ktx.v2020) // Check for the latest version
    implementation(libs.firebase.firestore.ktx.v2470) // Use the latest version available
    implementation (libs.picasso)
    implementation (libs.libphonenumber)
    implementation ("com.google.android.material:material:1.9.0")


}
