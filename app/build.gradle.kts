plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val k2000Native = providers.gradleProperty("k2000Native").orNull == "true"
android {
 namespace = "br.com.dashcamadas"
 compileSdk = 35
 buildFeatures { buildConfig = true }
 signingConfigs.getByName("debug") {
  enableV1Signing = true
  enableV2Signing = true
 }
 defaultConfig {
  applicationId = "br.com.dashcamadas"; minSdk = 24; targetSdk = 35; versionCode = 33
  versionName = if(k2000Native) "3.3.0-neural-usb-arm32-experimental" else "3.3.0-neural-adas-experimental"
  buildConfigField("boolean", "K2000_NATIVE", k2000Native.toString())
  if(k2000Native) ndk { abiFilters += "armeabi-v7a" }
 }
 sourceSets.getByName("main").jniLibs.srcDirs(if(k2000Native) listOf("src/k2000Native/jniLibs") else emptyList<String>())
 packaging { jniLibs { useLegacyPackaging = true } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies {
 implementation("org.tensorflow:tensorflow-lite:2.17.0")
 implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
 implementation("androidx.core:core:1.15.0")
 testImplementation("junit:junit:4.13.2")
 testRuntimeOnly("com.microsoft.onnxruntime:onnxruntime:1.20.0")
}
