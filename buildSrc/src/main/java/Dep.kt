object Dep {
    object Version {
        const val buildGradle = "3.5.3"
        const val kotlin = "1.3.50"
        const val junit = "4.12"
        const val espresso = "3.2.0"
        const val androidX = "1.1.0"
        const val constraintlayout = "1.1.3"
        const val material = "1.1.0-alpha07"
        const val glide = "4.11.0"
        const val exoPlayer = "2.8.4"
        const val coroutines = "1.3.3"
        const val testJunit = "1.1.1"
    }

    //plugin
    val pluginBuildGradle = "com.android.tools.build:gradle:${Version.buildGradle}"
    val pluginKotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}"

    //kotlin
    val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Version.kotlin}"

    //androidx
    val appcompat = "androidx.appcompat:appcompat:${Version.androidX}"
    val corex = "androidx.core:core-ktx:${Version.androidX}"
    val constraintlayout = "androidx.constraintlayout:constraintlayout:${Version.constraintlayout}"

    val material = "com.google.android.material:material:${Version.material}"
    val glide = "com.github.bumptech.glide:glide:${Version.glide}"
    val glideCompiler = "com.github.bumptech.glide:compiler:${Version.glide}"
    val exoPlayer = "com.google.android.exoplayer:exoplayer:${Version.exoPlayer}"

    //coroutines
    val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
    val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"

    //test
    val junit = "junit:junit:${Version.junit}"
    val espresso = "androidx.test.espresso:espresso-core:${Version.espresso}"
    val testJunit = "androidx.test.ext:junit:${Version.testJunit}"
}