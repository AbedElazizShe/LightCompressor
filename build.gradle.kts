buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven( url = "https://jitpack.io" )
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}
