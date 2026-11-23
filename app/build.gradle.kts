// Importa la clase que permite leer pares clave-valor desde el archivo de configuración local.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Procesa google-services.json y genera los recursos de configuración que utilizan los SDK de Firebase.
    alias(libs.plugins.google.services)
}

// Lee una URL local opcional sin exigir ni almacenar secretos dentro del código fuente.
val propiedadesServicio = Properties()
val archivoServicio = rootProject.file("servicio.properties")
if (archivoServicio.exists()) {
    archivoServicio.inputStream().use { flujo -> propiedadesServicio.load(flujo) }
}
val urlBaseServicio = propiedadesServicio.getProperty("URL_BASE_SERVICIO") ?: "https://jsonplaceholder.typicode.com/"

android {
    namespace = "com.daille.recordatorios"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.daille.recordatorios"
        // Los íconos adaptativos del proyecto requieren Android 8.0 (API 26) como mínimo.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Expone la URL configurada para Retrofit sin incluir secretos en el código fuente.
        buildConfigField("String", "URL_BASE_SERVICIO", "\"$urlBaseServicio\"")
    }

    buildTypes {
        debug {
            // Identifica el ambiente local de desarrollo sin que Compose conozca detalles de Firebase.
            buildConfigField("String", "ENTORNO_FIREBASE", "\"desarrollo\"")
        }
        release {
            // Identifica el ambiente reservado a pruebas controladas antes de preparar una entrega release.
            buildConfigField("String", "ENTORNO_FIREBASE", "\"pruebas\"")
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
        // Permite exponer la URL pública del servicio remoto sin incluir secretos en el código.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    // Usa el BoM oficial para mantener versiones compatibles entre Authentication y Firestore.
    implementation(platform(libs.firebase.bom))
    // Permite obtener la identidad que delimita los datos remotos de cada persona usuaria.
    implementation(libs.firebase.auth)
    // Implementa la fuente cloud de recordatorios dentro de la capa de datos.
    implementation(libs.firebase.firestore)
    // Permite esperar tareas de Firebase desde coroutines sin bloquear el hilo principal.
    implementation(libs.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
