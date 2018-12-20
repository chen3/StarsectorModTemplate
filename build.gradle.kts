import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.org.apache.xalan.internal.xsltc.cmdline.Compile
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import java.util.Properties
import java.io.InputStreamReader
import java.io.FileInputStream
import java.nio.file.Paths

buildscript {
    val jacksonVersion = "2.9.8"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
        classpath("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    }
}

plugins {
    kotlin("jvm").version("1.3.10").apply(false)
    id("net.linguica.maven-settings").version("0.5").apply(false)
}

loadProperty()

apply("local.gradle.kts")

allprojects {
    group = "cn.catgod.mx404.catgodsect"
    version = "0.0.0-SNAPSHOT"
}

subprojects {
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = getExtraStr("starsector.jre.version")!!
    }
    tasks.withType<JavaCompile> {
        sourceCompatibility = getExtraStr("starsector.jre.version")!!
        targetCompatibility = sourceCompatibility
    }
}

tasks.create("build") {
    this.dependsOn(":Loader:shadowJar")
    this.dependsOn(":Mod:shadowJar")

    doLast {
        val outDirPath: String = Paths.get(rootProject.rootDir.toString(), "out").toString()

        project.sync {
            from(Paths.get(project(":Mod").projectDir.toString(), "src", "main", "resources").toString())
            into(outDirPath)
        }
        project.copy {
            from(Paths.get(project(":Mod").projectDir.toString(),"build", "libs").toString())
            into(Paths.get(outDirPath, "jars").toString())
        }
        project.copy {
            from(Paths.get(project(":Loader").projectDir.toString(), "build", "libs" ).toString())
            into(Paths.get(outDirPath, "jars").toString())
        }
        File(outDirPath, "mod_info.json").writeText(createModInfoJson())
    }
}

tasks.create("install") {
    this.dependsOn("build")

    doLast {
        val dir: String? = getExtraStr("starsector.mod.dir")
        if (dir.isNullOrBlank()) {
            return@doLast
        }
        project.sync {
            from(File(rootProject.rootDir, "out").toString())
            into(Paths.get(dir, project.name).toString())
        }
    }
}

fun loadProperty() {
    for (name: String in arrayOf("project.properties", "local.properties")) {
        val properties = Properties()
        FileInputStream(File(rootProject.projectDir, name)).use {
            properties.load(InputStreamReader(it, "UTF-8"))
            properties.forEach {(name, value) ->
                this.extra[name.toString()] = value.toString()
            }
        }
    }
}

fun createModInfoJson(): String {
    val modInfo = ModInfo(
        id = rootProject.name.toLowerCase(),
        name = getExtraStr("mod.name")!!,
        author = getExtraStr("mod.author")!!,
        description = getExtraStr("mod.description")!!,
        version = rootProject.version.toString(),
        gameVersion = getExtraStr("starsector.version")!!,
        jars = arrayOf("jars/${project(":Loader").name}-${project(":Loader").version}.jar"),
        modPlugin = "${project.group}.Loader",
        modSuperLoader = ModInfo.RealModInfo(getExtraStr("mod.modPlugin")!!,
                                arrayOf("jars/${project(":Mod").name}-${project(":Mod").version}.jar"))
    )

    @Suppress("SpellCheckingInspection")
    val indenter: DefaultPrettyPrinter.Indenter = DefaultIndenter("    ", DefaultIndenter.SYS_LF)
    val printer = DefaultPrettyPrinter()
    printer.indentObjectsWith(indenter)
    printer.indentArraysWith(indenter)
    return ObjectMapper().writer(printer).writeValueAsString(modInfo)
}

fun getExtraStr(name: String): String? {
    if (!rootProject.extra.has(name)) {
        return null
    }
    return rootProject.extra[name].toString()
}

private data class ModInfo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val gameVersion: String,
    val jars: Array<String>,
    val modPlugin: String,
    val modSuperLoader : RealModInfo) {

    init {
        assert(!id.isEmpty())
        assert(!name.isEmpty())
        assert(!description.isEmpty())
        assert(!version.isEmpty())
        assert(!gameVersion.isEmpty())
        assert(!jars.isEmpty())
        assert(!modPlugin.isEmpty())
    }

    data class RealModInfo(
        val modPlugin: String,
        val jars: Array<String>
    ) {
        init {
            assert(!modPlugin.isEmpty())
            assert(!jars.isEmpty())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RealModInfo

            if (modPlugin != other.modPlugin) return false
            if (!jars.contentEquals(other.jars)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = modPlugin.hashCode()
            result = 31 * result + jars.contentHashCode()
            return result
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as ModInfo
        if (id != other.id || name != other.name || description != other.description
            || author != other.author || version != other.version
            || gameVersion != other.gameVersion || !jars.contentEquals(other.jars)
            || modPlugin != other.modPlugin || modSuperLoader != other.modSuperLoader) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + gameVersion.hashCode()
        result = 31 * result + jars.contentHashCode()
        result = 31 * result + modPlugin.hashCode()
        result = 31 * result + modSuperLoader.hashCode()
        return result
    }
}