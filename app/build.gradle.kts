import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
}

// Baca API_BASE_URL dari local.properties (file ini beda-beda tiap laptop & TIDAK di-push ke GitHub).
// Kalau belum diisi orang yang bersangkutan, default-nya pakai alamat emulator.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val apiBaseUrl: String = localProperties.getProperty("API_BASE_URL") ?: "http://10.0.2.2/casirku/api/"

android {
    namespace = "com.example.program_kasir"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.program_kasir"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    //res api
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Cetak struk ke printer thermal Bluetooth (protokol ESC/POS)
    implementation("com.github.DantSu:ESCPOS-ThermalPrinter-Android:3.3.0")

}
