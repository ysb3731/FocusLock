plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub Actions 가 넣어 주는 값. 로컬(Android Studio) 빌드에서는 없어도 됨.
val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val keystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")

android {
    namespace = "kr.focuslock.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.focuslock.app"
        minSdk = 26
        targetSdk = 36   // Google Play: 2026-08-31부터 신규 앱·업데이트는 API 36 이상
        versionCode = ciVersionCode
        versionName = "1.0.$ciVersionCode"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("upload") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) signingConfig = signingConfigs.getByName("upload")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// 외부 라이브러리 없이 Android 기본 API만 사용합니다.
dependencies {}
