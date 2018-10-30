import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    repositories {
        jcenter()
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.0")
    }

}

plugins {
    kotlin("jvm") version "1.3.0"
}

apply {
    plugin("application")
    plugin("kotlinx-serialization")
}

group = "me.sunnydaydev"
version = "1.0"

repositories {
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    mavenCentral()
}


configure<ApplicationPluginConvention> {
    mainClassName = "me.sunnydaydev.genom.AppKt"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.9.0")
}

tasks.withType<Jar> {

    manifest {
        attributes["Main-Class"] = "me.sunnydaydev.genom.AppKt"
    }

    from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}