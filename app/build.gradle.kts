plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.gms)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.chatee2e"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.chatee2e"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirst("lib/**/libsqlcipher.so")
        }
    }
}


dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //implementation(libs.sqlcipher)
    //implementation(libs.androidx.sqlite)

    //implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    //implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    //implementation("net.zetetic:sqlcipher-android:4.6.0@aar")
// Use the latest version
    //implementation("androidx.sqlite:sqlite:2.4.0")

    //implementation("net.zetetic:android-database-sqlcipher:4.6.0")

    // 2. Нужная версия SQLite KTX (обязательно 2.4.0 или новее для совместимости)
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("net.zetetic:sqlcipher-android:4.5.4")

    //implementation("androidx.biometric:biometric-ktx:1.4.0-alpha03") // Или 1.2.0 stable
    implementation("androidx.appcompat:appcompat:1.7.0") // Нужен для BiometricPrompt
    implementation(libs.androidx.biometric)

    implementation("com.google.firebase:firebase-messaging:23.0.0")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.code.gson:gson:2.10.1")
}