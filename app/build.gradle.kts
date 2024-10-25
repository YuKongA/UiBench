@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.BaseVariantOutputImpl


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.android.test.uibench"
    compileSdk = 35

    defaultConfig {
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    dependenciesInfo.includeInApk = false
    kotlin.jvmToolchain(17)
    packaging {
        applicationVariants.all {
            outputs.all {
                (this as BaseVariantOutputImpl).outputFileName = "UiBench-v$versionName($versionCode)-$name.apk"
            }
        }
        resources.excludes += "**"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
}