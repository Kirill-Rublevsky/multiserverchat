plugins { id("com.gradleup.shadow") }
dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
tasks.shadowJar { archiveBaseName.set("ovenquill-globalchat-backend"); archiveClassifier.set("") }
tasks.build { dependsOn(tasks.shadowJar) }
