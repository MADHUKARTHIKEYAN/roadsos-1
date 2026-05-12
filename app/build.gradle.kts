plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.carsosphase1"
    compileSdk = 36  // ← THIS FIX

    buildTypes {
    debug {
        buildConfigField("String", "AI_API_KEY", "\"AIzaSyBbbhYxuNDgv2roSgdLOGY54_v0X6xfDA4\"")
        buildConfigField("String", "MAPS_API_KEY", "\"your-maps-key\"")
    }
    release {
        buildConfigField("String", "AI_API_KEY", "\"your-prod-key\"")
        buildConfigField("String", "MAPS_API_KEY", "\"your-prod-maps-key\"")
    }
}

    defaultConfig {
        applicationId = "com.example.carsosphase1"
        minSdk = 24
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
