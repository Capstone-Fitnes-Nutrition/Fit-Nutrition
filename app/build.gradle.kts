import java.util.Properties
val properties = Properties()
properties.load(file("local.properties").inputStream())
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "sheridan.dheripu.fitnutrition"
    compileSdk = 36

    defaultConfig {
        applicationId = "sheridan.dheripu.fitnutrition"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FITBIT_CLIENT_ID",
            "\"${properties.getProperty("FITBIT_CLIENT_ID")}\"")
        buildConfigField("String", "FITBIT_CLIENT_SECRET",
            "\"${properties.getProperty("FITBIT_CLIENT_SECRET")}\"")
        buildConfigField("String", "FITBIT_REDIRECT_URI",
            "\"${properties.getProperty("FITBIT_REDIRECT_URI")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

//    val composeBom = platform("androidx.compose:compose-bom:2024.03.00")
//    implementation(composeBom)
//    androidTestImplementation(composeBom)

    implementation(platform("androidx.compose:compose-bom:2024.03.00"))


    // Compose Dependencies
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.room.compiler)
    implementation(libs.core.ktx)
    implementation(libs.androidx.navigation.compose)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")


//    Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")

    // added retrofit dependencies
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // added viewmodel dependencies
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
    }

}