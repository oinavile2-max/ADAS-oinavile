import java.security.MessageDigest

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

// User requirement: builds must include real neural models and pass genuine inference tests.
val neuralModels = mapOf(
 "src/main/assets/road_lanes_yolop_320.onnx" to "f93ced8254ccbb532ff9f9593e7b7d0022f28210831b615f5039066c9f5222fe",
 "src/main/assets/vehicles_ssd_mobilenet_v2.tflite" to "42fb3d70ffb7bb37dd518f730f7be784b831c2078f30c497d0019cc2e987fa26"
)
val verifyNeuralModelAssets = tasks.register("verifyNeuralModelAssets") {
 inputs.files(neuralModels.keys.map { layout.projectDirectory.file(it) })
 doLast {
  neuralModels.forEach { (path, expected) ->
   val model = layout.projectDirectory.file(path).asFile
   check(model.isFile) { "Required neural model missing: $path" }
   val digest = MessageDigest.getInstance("SHA-256").digest(model.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
   check(digest == expected) { "Required neural model altered: $path" }
  }
 }
}
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
 dependsOn(verifyNeuralModelAssets)
 inputs.files(neuralModels.keys.map { layout.projectDirectory.file(it) })
}
tasks.matching { it.name in setOf("assembleDebug", "assembleRelease", "packageDebug", "packageRelease") }.configureEach {
 dependsOn(verifyNeuralModelAssets, "testDebugUnitTest")
}
