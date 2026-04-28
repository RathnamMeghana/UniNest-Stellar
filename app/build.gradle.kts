plugins {
    alias(libs.plugins.android.application)

    alias(libs.plugins.kotlin.android)


    id("com.google.gms.google-services")

    id("com.chaquo.python")
    id("io.sentry.android.gradle") version "4.3.1"
}



android {
    namespace = "com.example.uninest"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.uninest"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }

    chaquopy {
        defaultConfig {
            version = "3.10"

            pip {

                install("numpy")
                install ("keras-preprocessing")


            }
        }
    }
    packagingOptions {
        pickFirst("lib/x86/libtensorflowlite_jni.so")
        pickFirst("lib/x86_64/libtensorflowlite_jni.so")
        pickFirst("lib/armeabi-v7a/libtensorflowlite_jni.so")
        pickFirst("lib/arm64-v8a/libtensorflowlite_jni.so")
    }

    flavorDimensions += listOf("userType")

    productFlavors {
        create("tenant") {
            dimension = "userType"
            applicationIdSuffix = ".tenant"
            versionNameSuffix = "-tenant"
        }
        create("agent") {
            dimension = "userType"
            applicationIdSuffix = ".agent"
            versionNameSuffix = "-agent"
        }
    }



    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    aaptOptions {
        noCompress("tflite")
        noCompress("pkl")
        noCompress("json")

    }
}

dependencies {
    // Standard Libraries (using your Version Catalog 'libs')
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // implementation(libs.play.services.measurement.api)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-appcheck")
    //implementation("com.google.firebase:firebase-appcheck-debug")

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.datastore:datastore:1.1.1")
    implementation ("com.google.firebase:firebase-storage")
    implementation ("io.sentry:sentry-android:7.6.0")
    implementation ("io.sentry:sentry-android-okhttp:7.6.0")

    //implementation ("com.google.firebase:firebase-appcheck:18.0.0")
    //implementation ("com.google.firebase:firebase-appcheck-debug:18.0.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")


    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.exifinterface:exifinterface:1.3.7")



    implementation ("org.tensorflow:tensorflow-lite:2.13.0")
    // Required for GRU / Bidirectional layers
    implementation ("org.tensorflow:tensorflow-lite-select-tf-ops:2.13.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")

    //implementation("com.google.android.gms:play-services-tflite-java:16.0.1")



    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.firebase.database)

    implementation ("com.applandeo:material-calendar-view:1.9.2")
    implementation ("com.google.android.material:material:1.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("com.google.firebase:firebase-appcheck-debug")

}

sentry {
    org.set("aoife-murphy")
    projectName.set("android")

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}

val ideTestClassesCompatibility = tasks.register("testClasses") {
    group = "verification"
    description = "Compatibility task for IDEs that request :app:testClasses on Android application modules."
    dependsOn(
        "compileTenantDebugJavaWithJavac",
        "compileAgentDebugJavaWithJavac",
        "compileTenantDebugUnitTestSources",
        "compileAgentDebugUnitTestSources"
    )
}
