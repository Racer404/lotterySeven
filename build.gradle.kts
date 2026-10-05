plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
}

group = "fund.racer"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        name = "xenondevsReleases"
        url = uri("https://repo.xenondevs.xyz/releases")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    implementation("xyz.xenondevs.invui:invui:2.5.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    build {
        dependsOn("shadowJar")
    }
}