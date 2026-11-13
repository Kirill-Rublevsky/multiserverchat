plugins { id("com.gradleup.shadow") }
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:4.3.0")
    annotationProcessor("com.velocitypowered:velocity-api:4.3.0")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
}
tasks.shadowJar {
    archiveBaseName.set("ovenquill-globalchat-velocity")
    archiveClassifier.set("")
    relocate("org.spongepowered.configurate", "ru.ovenquill.globalchat.lib.configurate")
    relocate("org.yaml.snakeyaml", "ru.ovenquill.globalchat.lib.snakeyaml")
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.build { dependsOn(tasks.shadowJar) }
