plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "br.com.dashcamadas"
 compileSdk = 35
 buildFeatures { buildConfig = true }
 defaultConfig { applicationId = "br.com.dashcamadas"; minSdk = 24; targetSdk = 35; versionCode = 31; versionName = "3.1.0-adas-experimental" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies { implementation("org.tensorflow:tensorflow-lite:2.17.0"); implementation("androidx.core:core:1.15.0"); testImplementation("junit:junit:4.13.2") }
