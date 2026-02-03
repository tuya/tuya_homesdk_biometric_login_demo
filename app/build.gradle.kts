plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.thingclips.smart.biometrics_login"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thingclips.smart.biometrics_login"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }
    packaging {
       jniLibs.pickFirsts.add("lib/*/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/*/libthing_security_algorithm.so")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("xichen.jks")
            storePassword = "xichen.dev"
            keyAlias = "xichen.key"
            keyPassword = "xichen.dev"
        }
        create("release") {
            storeFile = file("xichen.jks")
            storePassword = "xichen.dev"
            keyAlias = "xichen.key"
            keyPassword = "xichen.dev"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
        viewBinding = true
    }
}


configurations.all {
    exclude(group = "com.thingclips.smart", module = "thingsmart-modularCampAnno")
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("com.alibaba:fastjson:1.1.67.android")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:3.14.9")

    // App SDK 最新稳定安卓版：
    implementation("com.thingclips.smart:thingsmart:6.11.6")

    implementation("com.thingclips.smart:thingsmart-biometrics-login:9.9.9-LOCAL")
    implementation("androidx.biometric:biometric:1.1.0")

}