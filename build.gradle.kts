import org.gradle.jvm.tasks.Jar;

plugins {
    id("java")
    id("dev.architectury.loom") version ("1.9-SNAPSHOT")
    id("architectury-plugin") version ("3.4-SNAPSHOT")
    kotlin("jvm") version "2.2.20"
}

group = "org.ppvon"
val minecraftVersion = "1.21.1"
version = "1.4.0"

base {
    archivesName.set("ucp-fabric+mc${minecraftVersion}");
}

java {
    withJavadocJar()
    withSourcesJar()
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    silentMojangMappingsLicense()

    mixin {
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }

    runs {
        create("client2") {
            client()
            name("Client 2")
            // separate game directory so files/locks don't clash
            runDir("run2")
            // give it a unique username (dev/offline auth)
            programArgs("--username", "Tester2", "--uuid", "00000000-0000-0000-0000-000000000002")
            vmArgs("-Xmx2G")
        }
        create("client3") {
            client()
            name("Client 3")
            // separate game directory so files/locks don't clash
            runDir("run2")
            // give it a unique username (dev/offline auth)
            programArgs("--username", "Tester3", "--uuid", "00000000-0000-0000-0000-000000000002")
            vmArgs("-Xmx2G")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.ladysnake.org/releases")
    maven ("https://maven.terraformersmc.com/")
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("net.minecraft:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.18.1")

    modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:0.116.7+1.21.1")
    modImplementation(fabricApi.module("fabric-command-api-v2", "0.116.7+1.21.1"))
    modImplementation(fabricApi.module("fabric-resource-loader-v0", "0.116.7+1.21.1"))
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", "0.116.7+1.21.1"))
    modImplementation(fabricApi.module("fabric-networking-api-v1", "0.116.7+1.21.1"))
    modImplementation(fabricApi.module("fabric-entity-events-v1", "0.116.7+1.21.1"))
    modImplementation(fabricApi.module("fabric-events-interaction-v0", "0.116.7+1.21.1"))

    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")

    modImplementation("com.cobblemon:fabric:1.8.4+1.21.1")

    modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-base:6.1.2")
    modImplementation("dev.onyxstudios.cardinal-components-api:cardinal-components-entity:6.1.2")

    modCompileOnly("com.terraformersmc:modmenu:11.0.3")
    //modImplementation("com.terraformersmc:modmenu:11.0.3")

    implementation("com.google.code.gson:gson:2.11.0")


    /*
    modRuntimeOnly("dev.architectury:architectury-fabric:13.0.8")
    include(
        modRuntimeOnly(
            "maven.modrinth:admiral:0.4.10+1.21.1+fabric"
        )!!
    )
    modRuntimeOnly("maven.modrinth:rctapi:o44fzA4w")

    modImplementation("maven.modrinth:rad-gyms:3YDEmZwD")
     */
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(project.properties)
    }
}

tasks.named<JavaExec>("runClient") {
    args("--username", "UCP_Tester")
    //30815191-a90b-433a-aa64-e3e819bcc4be

    //args("--username", "UCP_Tester-2")
}

tasks.javadoc {
    include("org/ppvon/ultimateCobblemonProgression/api/**")

    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PUBLIC
}

tasks.named<Jar>("remapJar") {
    archiveFileName.set("ucp-fabric-${version}+mc${minecraftVersion}.jar")
}

tasks.withType<Jar>().configureEach {
    when (name) {
        "remapSourcesJar" -> archiveFileName.set("ucp-fabric-${version}+mc${minecraftVersion}-sources.jar")
        "javadocJar" -> archiveFileName.set("ucp-fabric-${version}+mc${minecraftVersion}-javadoc.jar")
    }
}