plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sudhanshu.tva"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sudhanshu.tva"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.5.0"

        // Real values injected via GitHub Actions secrets in Step 3 (relay production setup).
        // Until then these default to placeholders so local/CI builds still succeed.
        buildConfigField(
            "String", "RELAY_URL",
            "\"${System.getenv("RELAY_URL") ?: "https://replace-me.onrender.com"}\""
        )
        buildConfigField(
            "String", "RELAY_APP_SECRET",
            "\"${System.getenv("RELAY_APP_SECRET") ?: "dev-placeholder-secret"}\""
        )
    }

    signingConfigs {
        create("release") {
            // Step 20: only configures real signing if the keystore secrets are
            // present (CI with RELEASE_* env vars set). Falls back to no custom
            // signing config otherwise — local/dev builds still work unsigned.
            val storeFilePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking (relay communication)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Local profile storage (Step 6: identity engine)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.code.gson:gson:2.11.0")

    // Testing (Step 19)
    testImplementation("junit:junit:4.13.2")
}
