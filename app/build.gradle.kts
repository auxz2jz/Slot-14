plugins {
    id("com.android.application")
}

android {
    namespace = "com.zaksecurity.motiontracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zaksecurity.motiontracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.opencv:opencv:5.0.0.1")
}
