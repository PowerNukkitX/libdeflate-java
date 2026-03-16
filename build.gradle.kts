plugins {
    `maven-publish`
}

group = "org.powernukkitx"
version = "0.0.3.PNX-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // publish the module that actually contains the Java classes
            from(project(":libdeflate-java-core").components["java"])

            groupId = "org.powernukkitx"
            artifactId = "libdeflate-java"
            version = project.version.toString()

            pom {
                name.set("libdeflate-java")
                description.set("Java bindings for libdeflate used by PowerNukkitX")
                url.set("https://github.com/PowerNukkitX/libdeflate-java")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/PowerNukkitX/libdeflate-java.git")
                    developerConnection.set("scm:git:ssh://github.com/PowerNukkitX/libdeflate-java.git")
                    url.set("https://github.com/PowerNukkitX/libdeflate-java")
                }
            }
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
