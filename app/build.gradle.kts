import org.gradle.kotlin.dsl.libs

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("kotlin-kapt")
}

android {
    namespace = "dev.pegasus.phototemplates"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.pegasus.phototemplates"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        // To get the buildConfig object of our main application mostly used in App level class
        buildConfig = true
    }
}

dependencies {
    implementation(project(":template"))
    implementation(project(":stickers"))
    implementation(project(":regret"))

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.glide)

    // For Drawing on the bitmap
    implementation (libs.rasmview)
    // For EdgeToEdge feature
    implementation (libs.activity.ktx)
    // Koin for dependency injection
    implementation(libs.koin.android)

    // navigation components
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
}