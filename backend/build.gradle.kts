plugins { id("com.gradleup.shadow") }
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.135-stable")
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25) }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
tasks.shadowJar { archiveBaseName.set("ovenquill-globalchat-backend"); archiveClassifier.set("") }
tasks.build { dependsOn(tasks.shadowJar) }
