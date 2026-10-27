plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.ghostblueprints"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

// Release builds target Java 21. Paper 26.x APIs require Java 25 to compile against, so the
// forward-compatibility check raises the release level: -PjavaRelease=25.
val javaRelease = providers.gradleProperty("javaRelease").map(String::toInt).getOrElse(21)

java {
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
}

// Override to verify forward compatibility, e.g. -PpaperApi=26.3.build.226-stable (needs JDK 25).
val paperApi = providers.gradleProperty("paperApi").getOrElse("1.21.4-R0.1-SNAPSHOT")

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = javaRelease
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.4")
    }
}
