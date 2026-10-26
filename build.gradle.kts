plugins { id("net.fabricmc.fabric-loom") version "1.15.5"; kotlin("jvm") version "2.3.20" }

group = "net.ciyuan.lynxrelics"
version = "0.2.0-dev"
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)); withSourcesJar() }
kotlin { jvmToolchain(25) }
loom {
    runs {
        named("server") { runDir("run/server"); vmArg("-Dlynxrelics.probe=true") }
        named("client") { runDir("run/client") }
    }
}
repositories { mavenCentral(); maven("https://maven.fabricmc.net/") }

// Loom resolves the official named 26.3 SDK. The verified Elytra JAR is optional dev runtime only.
val elytraSlotJar = providers.gradleProperty("elytraSlotJar").orElse("E:/Minecraft/Lunar/mods/fabric-26.3/elytra-slot-1.2.jar")
val bundledRuntime by configurations.creating
dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("net.fabricmc.fabric-api:fabric-api:0.163.0+26.3")
    if(file(elytraSlotJar.get()).isFile) runtimeOnly(files(elytraSlotJar))
    implementation(kotlin("stdlib"))
    bundledRuntime(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-proc:none", "-Xlint:unchecked"))
}
tasks.processResources { filesMatching("fabric.mod.json") { expand("version" to project.version) } }
tasks.test { useJUnitPlatform(); maxParallelForks = 1; systemProperty("file.encoding", "UTF-8") }
tasks.named<JavaExec>("runClient") {
    if (providers.gradleProperty("clientBaseline").isPresent) {
        classpath = files(classpath.files.filterNot { it in sourceSets.main.get().output.files || it.name == file(elytraSlotJar.get()).name })
    }
    providers.gradleProperty("clientJava").orNull?.let { setExecutable(it) }
}
tasks.register("writeClientLaunch") {
    dependsOn("classes", "configureClientLaunch", "downloadAssets")
    doLast {
        val run = tasks.named<JavaExec>("runClient").get()
        val args = run.allJvmArgs + listOf("-classpath", run.classpath.asPath, run.mainClass.get()) + (run.args ?: emptyList())
        val output = file(".local/client-launch.args")
        output.parentFile.mkdirs()
        output.writeText(args.joinToString("\n") {
            if (it.startsWith("@") && file(it.drop(1)).isFile) file(it.drop(1)).readText()
            else "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } + "\n")
        println("Prepared local Java 25 client launch arguments; baseline=" + providers.gradleProperty("clientBaseline").isPresent)
    }
}
tasks.jar {
    archiveBaseName.set("LynxRelics")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(listOf("LICENSE", "NOTICE.md")) { into("META-INF") }
    from(bundledRuntime.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }
    from(bundledRuntime.map { zipTree(it) }) {
        include("META-INF/LICENSE*", "META-INF/NOTICE*");eachFile { path=name };includeEmptyDirs=false
        into("META-INF/licenses/kotlin")
    }
}
tasks.register<Zip>("distribution") {
    dependsOn(tasks.build)
    archiveFileName.set("LynxRelics-${project.version}-distribution.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(tasks.jar) { into("mods") }
    from(tasks.named("sourcesJar")) { into("sources") }
    from(projectDir) {
        include("README.md", "LICENSE", "NOTICE.md", "docs/**", "assets/models/**", "assets/icons/**", "assets/materials/**", "scripts/**", "src/**", "gradle/**", "gradlew.ps1", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "package.json")
    }
}
