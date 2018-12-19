import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow").version("4.0.3")
}

tasks.withType<ShadowJar> {
    classifier = ""
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
}