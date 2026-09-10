plugins {
    id("net.neoforged.moddev.legacyforge")
}

version = "${property("mod_version")}-${property("deps.minecraft")}-forge"
base.archivesName = property("archives_name") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
}

legacyForge {
    version = property("deps.minecraft") as String + "-" + property("deps.forge") as String
    validateAccessTransformers = true

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("archives_name") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/neoforge.mods.toml", "**/*.accesswidener")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod_version")}"))
        dependsOn("build")
    }

    jar {
        finalizedBy("reobfJar")
    }
}

java {
    withSourcesJar()
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21
    else if (stonecutter.eval(stonecutter.current.version, ">=1.18")) JavaVersion.VERSION_17
    else if (stonecutter.eval(stonecutter.current.version, ">=1.17")) JavaVersion.VERSION_16
    else JavaVersion.VERSION_1_8
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "mod_id" to project.property("archives_name") as String,
        "mod_name" to project.property("mod_name") as String,
        "mod_version" to project.property("mod_version") as String,
        "mod_license" to project.property("mod_license") as String,
    )

    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
}
