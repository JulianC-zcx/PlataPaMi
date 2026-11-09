plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.platapami.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.platapami.app"
        minSdk = 26            // Android 8.0+ (ficha técnica)
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }
    buildTypes {
        debug {
            // 10.0.2.2 = tu PC visto desde el emulador de Android Studio (XAMPP/WAMP).
            // En un celular físico usa la IP de tu PC, p. ej. "http://192.168.1.50/platapami_backend/public/api/"
            buildConfigField("String", "API_BASE", "\"http://10.0.2.2/backend/public/api/\"")
            manifestPlaceholders["usesCleartext"] = "true"
        }
        release {
            // Producción: SOLO HTTPS (ficha técnica, sección 9). Cambia el dominio por el de tu servidor.
            buildConfigField("String", "API_BASE", "\"https://api.tudominio.com/api/\"")
            manifestPlaceholders["usesCleartext"] = "false"
            isMinifyEnabled = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")      // conexión HTTP(S) con la API PHP
    implementation("io.coil-kt:coil-compose:2.6.0")           // imágenes de comprobantes
}
