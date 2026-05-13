val amapApiKeyProvider = providers.gradleProperty("AMAP_API_KEY")
    .orElse(providers.environmentVariable("AMAP_API_KEY"))
    .orElse("")

val backendBaseUrlProvider = providers.gradleProperty("SAFEWALK_BACKEND_BASE_URL")
    .orElse(providers.environmentVariable("SAFEWALK_BACKEND_BASE_URL"))
    .orElse("")

val clerkPublishableKeyProvider = providers.gradleProperty("CLERK_PUBLISHABLE_KEY")
    .orElse(providers.environmentVariable("CLERK_PUBLISHABLE_KEY"))
    .orElse("")

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
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs/amap")
        }
    }
}

dependencies {
    implementation(files("libs/amap/AMap3DMap_11.1.001_AMapSearch_9.7.4_AMapLocation_11.1.001_20260402.jar"))
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
