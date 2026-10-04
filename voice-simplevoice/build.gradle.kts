// Simple Voice Chat -> PV-Transcribe core adapter. Uses only the Simple Voice Chat plugin API,
// which is the same on Bukkit/Paper, Fabric and NeoForge.
description = "SVC-Transcribe Simple Voice Chat adapter"

repositories {
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    implementation(project(":core"))
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${property("voicechatApiVersion")}")
}
