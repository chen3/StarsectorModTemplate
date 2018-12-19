import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow").version("4.0.3")
}

tasks.withType<ShadowJar> {
    classifier = ""
}

repositories {
    maven {
        name = "starsector-repository"
        setUrl(rootProject.extra["starsector-repository.url"].toString())
        if (!(rootProject.extra.has("starsector-repository.disableCredentials")
            && (rootProject.extra["starsector-repository.disableCredentials"].toString().toBoolean()))) {
            credentials {
                username = rootProject.extra["starsector-repository.username"].toString()
                password = rootProject.extra["starsector-repository.password"].toString()
            }
        }
    }
}

project.getTasksByName("compileJava", false).first().dependsOn(tasks.create("initProject") {
    doFirst {
        val javaFile = File(project.projectDir,
            "src/main/java/${project.group.toString().replace(".", "/")}/Loader.java")
        if (javaFile.exists()) {
            return@doFirst
        }
        javaFile.parentFile.mkdirs()
        javaFile.writeText(loaderJavaFileContent)
    }
})

dependencies {
    compileOnly("cn.catgod.mx404.modsuperloader", "api", "1.0.0-RC2")
}

val loaderJavaFileContent: String by lazy {
    """package ${project.group};

import cn.catgod.mx404.modsuperloader.ProxyBaseModPlugin;

@SuppressWarnings("unused")
public class Loader extends ProxyBaseModPlugin {
}"""
}