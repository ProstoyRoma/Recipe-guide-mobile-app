
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.chaquo.python")
}

android {


    namespace = "com.example.recipeguide"
    compileSdk = 35

    defaultConfig {
        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        applicationId = "com.example.recipeguide"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


}


chaquopy {
    defaultConfig {
        version = "3.13"
        pip {
            install ("numpy")
            install ("unidecode")
            install ("joblib")
            install ("nltk")
        }
    }

}


dependencies {

    implementation ("com.readystatesoftware.sqliteasset:sqliteassethelper:2.0.1")
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4") // MotionLayout
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.itextpdf:itext7-core:7.2.5")

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    implementation ("com.google.mlkit:translate:17.0.1")

    implementation ("com.cloudinary:cloudinary-android:3.0.2")

    implementation ("com.github.bumptech.glide:glide:4.16.0")

    implementation ("com.google.code.gson:gson:2.8.9")

    implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.9.0") // 🔥 Flex-операторы
    implementation ("org.tensorflow:tensorflow-lite:2.9.0") // Основной TFLite
    //implementation ("org.tensorflow:tensorflow-lite:2.10.0")

    implementation ("com.yandex.android:mobileads:7.16.0")
    implementation("com.google.android.gms:play-services-ads:24.6.0")

    implementation ("androidx.work:work-runtime:2.7.1")
}
