import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val amapApiKeyFromLocalProperties = providers.provider {
    localProperties.getProperty("amap.key")?.trim().orEmpty()
}

val amapApiKeyProvider = providers.gradleProperty("amap.key")
    .orElse(providers.gradleProperty("AMAP_API_KEY"))
    .orElse(providers.environmentVariable("AMAP_API_KEY"))
    .orElse(amapApiKeyFromLocalProperties)
    .orElse("")

val backendBaseUrlProvider = providers.gradleProperty("SAFEWALK_BACKEND_BASE_URL")
    .orElse(providers.environmentVariable("SAFEWALK_BACKEND_BASE_URL"))
    .orElse(providers.provider {
        localProperties.getProperty("SAFEWALK_BACKEND_BASE_URL").orEmpty()
    })
    .orElse("")

val clerkPublishableKeyProvider = providers.gradleProperty("CLERK_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("CLERK_PUBLISHABLE_KEY"))
    .orElse("")

val forceAmapRenderingOnEmulatorProvider = providers.gradleProperty("SAFEWALK_FORCE_AMAP_RENDERING_ON_EMULATOR")
    .orElse(providers.environmentVariable("SAFEWALK_FORCE_AMAP_RENDERING_ON_EMULATOR"))
    .map { it.toBooleanStrictOrNull() ?: false }
    .orElse(false)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.celestine.safewalk"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.celestine.safewalk"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKeyProvider.get()
        buildConfigField(
            "String",
            "SAFEWALK_BACKEND_BASE_URL",
            "\"${backendBaseUrlProvider.get()}\""
        )
        buildConfigField(
            "String",
            "CLERK_PUBLISHABLE_KEY",
            "\"${clerkPublishableKeyProvider.get()}\""
        )
        buildConfigField(
            "Boolean",
            "SAFEWALK_FORCE_AMAP_RENDERING_ON_EMULATOR",
            forceAmapRenderingOnEmulatorProvider.get().toString()
        )
        buildConfigField(
            "Boolean",
            "HAS_AMAP_API_KEY",
            amapApiKeyProvider.map { it.isNotBlank() }.get().toString()
        )
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    implementation(libs.amap.bundle)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.clerk.android.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
