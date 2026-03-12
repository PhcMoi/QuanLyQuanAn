plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android") version "1.9.0"
}

android {
    namespace = "com.example.qlquanan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.qlquanan"
        // Nâng minSdk lên 26 để hỗ trợ MethodHandle yêu cầu bởi Apache POI/Log4j
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            // Loại bỏ các tệp tin gây xung đột khỏi bộ đóng gói APK
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/ASL2.0"
        }

    }

    dependencies {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)
        implementation(files("libs/jtds-1.3.1.jar"))

        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)

        implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

        // Cấu hình Apache POI và loại trừ log4j-api gây lỗi MethodHandle trên một số môi trường
        implementation("org.apache.poi:poi-ooxml:5.2.3") {
            exclude(group = "org.apache.logging.log4j", module = "log4j-api")
        }

        // Sử dụng cầu nối log4j sang slf4j để tránh xung đột
        implementation("org.apache.logging.log4j:log4j-to-slf4j:2.19.0")
    }
}