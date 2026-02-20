import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

val ktorVersion = "2.3.10"

// ═══════════════════════════════════════════════════════════════════════════════
// Load environment variables from .env file
// ═══════════════════════════════════════════════════════════════════════════════
val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}
// Clean up the values (remove quotes and extra spaces)
val webClientId = envProperties.getProperty("WEB_CLIENT_ID")
    ?.replace("\"", "")
    ?.trim() ?: ""
val tripoApiKey = envProperties.getProperty("TRIPO3D_API_KEY")
    ?.replace("\"", "")
    ?.trim() ?: ""
val mapsApiKey = envProperties.getProperty("MAPS_API_KEY")
    ?.replace("\"", "")
    ?.trim() ?: ""
val anylineApiKey = envProperties.getProperty("ANYLINE_LICENSE_KEY")
    ?.replace("\"", "")
    ?.trim() ?: ""
val michelinApiKey = envProperties.getProperty("MICHELIN_API_KEY")
    ?.replace("\"", "")
    ?.trim() ?: ""

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            
            // ═══════════════════════════════════════════════════════════════
            // CameraX - Modern camera API for multi-angle tyre capture
            // Using stable 1.4.0 release for production reliability
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.camera:camera-core:1.4.0")
            implementation("androidx.camera:camera-camera2:1.4.0")
            implementation("androidx.camera:camera-lifecycle:1.4.0")
            implementation("androidx.camera:camera-view:1.4.0")
            
            // ═══════════════════════════════════════════════════════════════
            // ML Kit Object Detection - Tyre detection and framing assist
            // ═══════════════════════════════════════════════════════════════
            implementation("com.google.mlkit:object-detection:17.0.1")
            
            // ═══════════════════════════════════════════════════════════════
            // TensorFlow Lite - Custom tyre defect detection model
            // ═══════════════════════════════════════════════════════════════
            implementation("org.tensorflow:tensorflow-lite:2.14.0")
            implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
            implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
            // Task Vision API: Modern ObjectDetector + TensorImage (NO manual ByteBuffers)
            implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4") {
                exclude(group = "org.tensorflow", module = "tensorflow-lite")
            }
            
            // ═══════════════════════════════════════════════════════════════
            // Room Database - Local storage for tyre images and metadata
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.room:room-runtime:2.6.1")
            implementation("androidx.room:room-ktx:2.6.1")
            
            // ═══════════════════════════════════════════════════════════════
            // DataStore - User preferences and settings
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.datastore:datastore-preferences:1.1.1")
            
            // ═══════════════════════════════════════════════════════════════
            // Ktor HTTP Client - For cloud-based 3D reconstruction APIs
            // ═══════════════════════════════════════════════════════════════
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            
            // ═══════════════════════════════════════════════════════════════
            // SceneView - Modern 3D rendering library (Sceneform successor)
            // This is the ONLY stable, maintained library for GLB/GLTF on Android
            // Supports Jetpack Compose and is XR-ready
            // ═══════════════════════════════════════════════════════════════
            implementation("io.github.sceneview:sceneview:2.2.1")
            
            // ═══════════════════════════════════════════════════════════════
            // ARSceneView - AR extension for SceneView with ARCore integration
            // Provides plane detection, anchor placement, and AR rendering
            // ═══════════════════════════════════════════════════════════════
            implementation("io.github.sceneview:arsceneview:2.2.1")
            
            // ═══════════════════════════════════════════════════════════════
            // ARCore - Google's AR platform for plane detection and tracking
            // Required for AR session management and world understanding
            // ═══════════════════════════════════════════════════════════════
            implementation("com.google.ar:core:1.47.0")
            
            // ═══════════════════════════════════════════════════════════════
            // Google Sign-In - Real authentication with Google accounts
            // Using Credential Manager for modern Android 14+ auth flow
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.credentials:credentials:1.3.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
            
            // ═══════════════════════════════════════════════════════════════
            // ExifInterface - Read/write image metadata for tyre positioning
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.exifinterface:exifinterface:1.3.7")
            
            // ═══════════════════════════════════════════════════════════════
            // WorkManager - Background processing for 3D model generation
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.work:work-runtime-ktx:2.10.0")
            
            // ═══════════════════════════════════════════════════════════════
            // Biometric - Modern biometric authentication (face, fingerprint, iris)
            // Uses unified BiometricPrompt API for all biometric types
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.biometric:biometric:1.2.0-alpha05")
            
            // Swipe refresh for pull-to-refresh UI
            implementation("androidx.compose.material:material:1.7.8")
            
            // Kotlinx Serialization for JSON handling
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            
            // ═══════════════════════════════════════════════════════════════
            // Jetpack Glance - Modern Android App Widgets with Compose
            // "The Samsung Style" Home Screen Widget
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.glance:glance-appwidget:1.1.0")
            implementation("androidx.glance:glance-material3:1.1.0")

            // ═══════════════════════════════════════════════════════════════
            // Lottie Compose - High-quality animations for onboarding
            // ═══════════════════════════════════════════════════════════════
            implementation("com.airbnb.android:lottie-compose:6.4.0")
            
            // ═══════════════════════════════════════════════════════════════
            // ConstraintLayout Compose - For MotionLayout-like transitions
            // ═══════════════════════════════════════════════════════════════
            implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
            
            // ═══════════════════════════════════════════════════════════════
            // Accompanist Permissions - Easy runtime permission handling
            // ═══════════════════════════════════════════════════════════════
            implementation("com.google.accompanist:accompanist-permissions:0.34.0")
            
            // ═══════════════════════════════════════════════════════════════
            // Google Maps SDK for Android - Map display and interaction
            // ═══════════════════════════════════════════════════════════════
            implementation("com.google.maps.android:maps-compose:4.3.3")
            implementation("com.google.android.gms:play-services-maps:19.0.0")
            implementation("com.google.android.gms:play-services-location:21.2.0")
            
            // ═══════════════════════════════════════════════════════════════
            // Places SDK for Android - Search nearby service centers
            // ═══════════════════════════════════════════════════════════════
            implementation("com.google.android.libraries.places:places:4.1.0")

            // ═══════════════════════════════════════════════════════════════
            // Anyline TireTread SDK - Professional tyre tread depth scanning
            // Maven: https://europe-maven.pkg.dev/anyline-ttr-sdk/maven
            // Requires ANYLINE_LICENSE_KEY in local.properties / .env
            // ═══════════════════════════════════════════════════════════════
            implementation("io.anyline.tiretread.sdk:shared:14.0.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Supabase for authentication and backend
            implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.4")
            implementation("io.github.jan-tennert.supabase:auth-kt:3.1.4")
            implementation("io.github.jan-tennert.supabase:compose-auth:3.1.4")
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            
            // Kotlinx coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        
        // ═══════════════════════════════════════════════════════════════════
        // Inject environment variables from .env file into BuildConfig
        // Access via: BuildConfig.WEB_CLIENT_ID, BuildConfig.TRIPO3D_API_KEY, BuildConfig.MAPS_API_KEY
        // ═══════════════════════════════════════════════════════════════════
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        buildConfigField("String", "TRIPO3D_API_KEY", "\"$tripoApiKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "ANYLINE_LICENSE_KEY", "\"$anylineApiKey\"")
        buildConfigField("String", "MICHELIN_API_KEY", "\"$michelinApiKey\"")

        // Manifest placeholders for API keys
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    
    // Enable BuildConfig generation
    buildFeatures {
        buildConfig = true
    }
    // Prevent compression of tflite files for efficient memory-mapping
    aaptOptions {
        noCompress("tflite")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    // Room compiler for KSP
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")
}

configurations.configureEach {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.ktor") {
                useVersion(ktorVersion)
                because("Anyline 14.0.0 depends on Ktor 2.3.10 (HttpRequestRetry class)")
            }
        }
    }
}
