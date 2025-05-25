import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.geminiviews"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.geminiviews"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 读取 local.properties 文件
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        // 从 properties 中获取 API_KEY
        val apiKey = properties.getProperty("apiKey")
            ?: System.getenv("apiKey") // 作为一个回退，也可以从环境变量获取

        if (apiKey == null) {
            throw GradleException("API_KEY not found. Please set it in local.properties or as an environment variable.")
        }
        buildConfigField("String", "apiKey", "\"$apiKey\"") // 生成 BuildConfig.apiKey 字段

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.generativeai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // RecyclerView
//    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Markwon (用于Markdown渲染)
    implementation("io.noties.markwon:core:4.6.2")
    // 如果Gemini回复包含图片、表格等，可能需要更多Markwon模块
    implementation("io.noties.markwon:image-coil:4.6.2")
    implementation("io.noties.markwon:image-picasso:4.6.2")

//    implementation("io.noties.markwon:core:4.6.2")
//    implementation("io.noties.markwon:editor:4.6.2")
//    implementation("io.noties.markwon:ext-latex:4.6.2")
//    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
//    implementation("io.noties.markwon:ext-tasklist:4.6.2")
//    implementation("io.noties.markwon:html:4.6.2")
//    implementation("io.noties.markwon:image:4.6.2")
//    implementation("io.noties.markwon:image-coil:4.6.2")
//    implementation("io.noties.markwon:image-glide:4.6.2")
//    implementation("io.noties.markwon:image-picasso:4.6.2")
//    implementation("io.noties.markwon:inline-parser:4.6.2")
//    implementation("io.noties.markwon:linkify:4.6.2")
//    implementation("io.noties.markwon:recycler:4.6.2")
//    implementation("io.noties.markwon:recycler-table:4.6.2")
//    implementation("io.noties.markwon:simple-ext:4.6.2")
//    implementation("io.noties.markwon:syntax-highlight:4.6.2")
}