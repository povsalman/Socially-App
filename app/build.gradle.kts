plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.salmankhan.i221285"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.salmankhan.i221285"
        minSdk = 29
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
}

dependencies {
    // Firebase BoM coordinates Firebase SDK versions
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")
    // Firebase Realtime Database (needed later for profile flag/stories)
    implementation("com.google.firebase:firebase-database-ktx")
    // Firebase Cloud Messaging for push notifications
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.material:material:1.8.0")
    // Agora Video SDK for real-time video calls
    implementation("io.agora.rtc:full-sdk:4.4.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
}