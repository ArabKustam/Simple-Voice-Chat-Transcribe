plugins {
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

description = "SVC-Transcribe for Paper / Spigot"

repositories {
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":engine-vosk"))
    implementation(project(":engine-tone"))
    implementation(project(":engine-cloud"))
    implementation(project(":voice-simplevoice"))

    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    testImplementation("org.yaml:snakeyaml:2.2")
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${property("voicechatApiVersion")}")
}

// The plugin is compiled to Java 17 bytecode (runs on 1.20.x servers too) against the newest Paper API,
// which is published for Java 21. Only API that exists since 1.20 is used.
configurations.named("compileClasspath") {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version,
            "voskVersion" to project.property("voskVersion"),
            "onnxRuntimeVersion" to project.property("onnxRuntimeVersion"),
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveBaseName.set("SVC-Transcribe")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        // Vosk + JNA are downloaded by the server from Maven Central (plugin.yml "libraries"),
        // Plasmo Voice and Paper are provided at runtime, so only our own modules end up in the jar.
        dependencies {
            include(project(":api"))
            include(project(":core"))
            include(project(":engine-vosk"))
            include(project(":engine-tone"))
            include(project(":engine-cloud"))
            include(project(":voice-simplevoice"))
        }
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }

    jar {
        archiveClassifier.set("plain")
    }

    build {
        dependsOn(shadowJar)
    }

    // `gradlew runServer` starts a local test server with this plugin; put Simple Voice Chat into
    // platform-paper/run/plugins (you will be asked to accept the Minecraft EULA there).
    runServer {
        minecraftVersion("1.21.11")
    }
}
