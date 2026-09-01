plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

val bootstrapToken = providers.gradleProperty("bootstrapToken")
  .orElse(providers.environmentVariable("BOOTSTRAP_TOKEN"))
  .orElse("")
val serverUrl = providers.gradleProperty("serverUrl")
  .orElse(providers.environmentVariable("SERVER_URL"))
  .orElse("https://conbotbtdi.onrender.com")

android {
  namespace = "com.conbot.client"
  compileSdk = 35
  defaultConfig {
    applicationId = "com.conbot.client"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "SERVER_URL", "\"${serverUrl.get().trimEnd('/').replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    buildConfigField("String", "PROTOCOL_VERSION", "\"1\"")
  }
  buildTypes {
    debug { buildConfigField("String", "BOOTSTRAP_TOKEN", "\"${bootstrapToken.get().replace("\\", "\\\\").replace("\"", "\\\"")}\"") }
    release { isMinifyEnabled = true; buildConfigField("String", "BOOTSTRAP_TOKEN", "\"\""); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
  }
  buildFeatures { compose = true; buildConfig = true }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
  packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2025.01.01"))
  implementation("androidx.activity:activity-compose:1.10.0")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}
