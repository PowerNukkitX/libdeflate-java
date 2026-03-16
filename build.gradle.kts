plugins {
    java
    `maven-publish`
}

group = "org.powernukkitx"
version = "0.0.3.PNX-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "org.powernukkitx"
            artifactId = "libdeflate-java"
            version = "0.0.3.PNX-SNAPSHOT"
        }
    }

    repositories {
        maven {
            name = "pnx"
            url = uri("https://repo.powernukkitx.org/releases")

            credentials {
                username = providers.gradleProperty("pnxUsername")
                    .orElse(providers.environmentVariable("PNX_REPO_USERNAME"))
                    .orNull
                password = providers.gradleProperty("pnxPassword")
                    .orElse(providers.environmentVariable("PNX_REPO_PASSWORD"))
                    .orNull
            }
        }
    }
}
