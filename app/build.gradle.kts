plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rafiq.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rafiq.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ===== AndroidX الأساسية =====
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.activity:activity-ktx:1.9.2")

    // ===== Lifecycle & ViewModel =====
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // ===== Coroutines =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // ===== Room Database =====
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // ===== Networking =====
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== JSON =====
    implementation("org.json:json:20240303")

    // ===== Glide (تحميل الصور) =====
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ===== ML Kit لاكتشاف الوجه (مطلوب لـ FaceAnalyzer.kt) =====
    implementation("com.google.mlkit:face-detection:16.1.7")

    // ===== Security (لتشفير الإعدادات) =====
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ===== WorkManager (للمهام المجدولة مثل تحية الصباح) =====
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ===== Preference =====
    implementation("androidx.preference:preference-ktx:1.2.1")
}
