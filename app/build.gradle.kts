import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder

plugins { id("com.android.application") }
val k2000Native = providers.gradleProperty("k2000Native").orNull == "true"
android {
 namespace = "br.com.dashcamadas"
 compileSdk = 37
 compileSdkMinor = 2
 buildFeatures { buildConfig = true }
 signingConfigs.getByName("debug") {
  enableV1Signing = true
  enableV2Signing = true
 }
 defaultConfig {
  applicationId = "br.com.dashcamadas"; minSdk = 25; targetSdk = 37; versionCode = 38
  versionName = if(k2000Native) "3.8.0-camera-orientation-rec-neural-nwd-k2001n-experimental" else "3.8.0-camera-orientation-rec-neural-experimental"
  buildConfigField("boolean", "K2000_NATIVE", k2000Native.toString())
  if(k2000Native) ndk { abiFilters += "armeabi-v7a" }
 }
 sourceSets.getByName("main").jniLibs.srcDirs(if(k2000Native) listOf("src/k2000Native/jniLibs") else emptyList<String>())
 packaging { jniLibs { useLegacyPackaging = true } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
 implementation("com.google.ai.edge.litert:litert:1.4.2")
 implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")
 implementation("androidx.core:core:1.15.0")
 implementation("androidx.activity:activity:1.13.0")
 testImplementation("junit:junit:4.13.2")
 testRuntimeOnly("com.microsoft.onnxruntime:onnxruntime:1.31.0")
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

// Validate the shared runtime libraries before any future APK packaging, including 16 KB devices.
val verifyNativePageAlignment = tasks.register("verifyNativePageAlignment") {
 dependsOn("mergeDebugNativeLibs")
 val nativeRoot = layout.buildDirectory.dir("intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib")
 inputs.dir(nativeRoot)
 doLast {
  val root = nativeRoot.get().asFile
  check(root.isDirectory) { "Merged Android native libraries missing" }
  var checked = 0
  listOf("arm64-v8a", "x86_64").forEach { abi ->
   root.resolve(abi).listFiles()?.filter { it.extension == "so" }?.forEach { library ->
    val bytes = library.readBytes()
    check(bytes.size >= 64 && bytes[0] == 0x7f.toByte() && bytes[1] == 69.toByte() && bytes[2] == 76.toByte() && bytes[3] == 70.toByte() && bytes[4] == 2.toByte()) { "Invalid ELF64: $library" }
    val elf = ByteBuffer.wrap(bytes).order(if (bytes[5] == 1.toByte()) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
    val start = elf.getLong(32).toInt()
    val size = elf.getShort(54).toInt() and 65535
    val count = elf.getShort(56).toInt() and 65535
    var loads = 0
    repeat(count) { index ->
     val header = start + index * size
     check(header >= 0 && header + 56 <= bytes.size) { "Invalid ELF program header: $library" }
     if (elf.getInt(header) == 1) {
      loads++
      val offset = elf.getLong(header + 8)
      val address = elf.getLong(header + 16)
      val alignment = elf.getLong(header + 48)
      check(alignment >= 16384L && offset % 16384L == address % 16384L) { "Native library does not support 16 KB memory pages: $library" }
     }
    }
    check(loads > 0) { "ELF has no loadable segments: $library" }
    checked++
   }
  }
  logger.lifecycle("16 KB ELF alignment verified for $checked merged Android 64-bit libraries before ABI packaging filters.")
 }
}
tasks.matching { it.name in setOf("assembleDebug", "assembleRelease", "packageDebug", "packageRelease") }.configureEach {
 dependsOn(verifyNativePageAlignment)
}
