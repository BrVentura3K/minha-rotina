import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.minharotina"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.brventura3k.rotina"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    // Chave fixa: todas as versões precisam ser assinadas com ela para
    // atualizar por cima e bater com o registro no Android Developer Console.
    // Na compilação do GitHub ela vem dos Secrets do repositório; no PC, de
    // um keystore.properties local (fora do git).
    val chave = Properties().apply {
        val local = rootProject.file("app/keystore.properties")
        if (local.exists()) local.inputStream().use { load(it) }
    }
    fun valor(prop: String, env: String): String? = System.getenv(env) ?: chave.getProperty(prop)
    val arquivoChave = valor("storeFile", "MR_KEYSTORE_FILE")

    signingConfigs {
        create("fixa") {
            if (arquivoChave != null) {
                storeFile = file(arquivoChave)
                storePassword = valor("storePassword", "MR_KEYSTORE_PASSWORD")
                keyAlias = valor("keyAlias", "MR_KEY_ALIAS") ?: "minharotina"
                keyPassword = valor("keyPassword", "MR_KEYSTORE_PASSWORD")
            }
        }
    }
    val assinatura = if (arquivoChave != null) signingConfigs.getByName("fixa") else signingConfigs.getByName("debug")

    buildTypes {
        debug {
            signingConfig = assinatura
        }
        release {
            isMinifyEnabled = false
            signingConfig = assinatura
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
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
