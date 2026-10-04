pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
}

rootProject.name = "SVC-Transcribe"

include(
    "api",
    "core",
    "engine-vosk",
    "engine-tone",
    "engine-cloud",
    "voice-simplevoice",
    "platform-paper",
)
