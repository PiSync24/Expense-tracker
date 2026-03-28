plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dhiraj.expensetracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dhiraj.expensetracker"
        minSdk = 24  // Android 7.0 (covers ~95% of devices)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
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
        compose = true  // Enable Jetpack Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Jetpack Compose (Modern UI toolkit)
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Room Database (Local storage)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines (For background tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel (Manages UI-related data)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    //Menu bar
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")


}

/*
DEPENDENCIES EXPLAINED:

1. CORE LIBRARIES:
   - core-ktx: Kotlin extensions for Android
   - lifecycle-runtime-ktx: Lifecycle-aware components

2. JETPACK COMPOSE:
   - Modern declarative UI toolkit
   - Replaces XML layouts with Kotlin code
   - Makes UI development much faster
   - material3: Material Design 3 components

3. ROOM DATABASE:
   - SQLite wrapper for Android
   - Type-safe database access
   - Automatic SQL generation
   - room-ktx: Kotlin extensions (coroutines support)
   - room-compiler: Generates database code at compile time

4. COROUTINES:
   - Handle background tasks (database, network)
   - Prevents UI freezing
   - Much simpler than threads/AsyncTask

5. VIEWMODEL:
   - Survives configuration changes (screen rotation)
   - Separates UI from business logic
   - Recommended architecture pattern

WHY THESE LIBRARIES?
- Room: Best practice for local data storage
- Compose: Modern, recommended by Google
- Coroutines: Industry standard for async operations
- All these work together seamlessly!
*/

