import java.nio.file.Paths
import java.io.ByteArrayOutputStream
import java.util.Locale
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    `java-library`
    `maven-publish`
}

group = "org.powernukkitx"
version = "0.0.3.PNX-SNAPSHOT"

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.register("compileNatives") {
    val jniTempPath = Paths.get(project.rootDir.toString(), "tmp")

    doLast {
        val env = hashMapOf("LIB_NAME" to "libdeflate_jni")
        env.putAll(System.getenv())

        when {
            Os.isFamily(Os.FAMILY_MAC) -> {
                if (System.getenv("CC") == null) env["CC"] = "clang"
                val byteOut = ByteArrayOutputStream()
                project.exec {
                    commandLine = listOf("/usr/libexec/java_home")
                    standardOutput = byteOut
                }
                env["JAVA_HOME"] = byteOut.toString("UTF-8").trim()
                env["DYLIB_SUFFIX"] = "dylib"
                env["JNI_PLATFORM"] = "darwin"
                env["LIB_DIR"] = Paths.get(jniTempPath.toString(), "compiled", "darwin", System.getProperty("os.arch")).toString()
                env["OBJ_DIR"] = Paths.get(jniTempPath.toString(), "objects", "darwin", System.getProperty("os.arch")).toString()
                env["CFLAGS"] = "-O2 -fomit-frame-pointer -Werror -Wall -fPIC -flto"

                exec {
                    executable = "make"
                    args = listOf("clean", "all")
                    environment = env
                }
            }

            Os.isFamily(Os.FAMILY_UNIX) -> {
                if (System.getenv("CC") == null) env["CC"] = "gcc"
                val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)
                env["DYLIB_SUFFIX"] = "so"
                env["JNI_PLATFORM"] = osName
                env["LIB_DIR"] = Paths.get(jniTempPath.toString(), "compiled", osName, System.getProperty("os.arch")).toString()
                env["OBJ_DIR"] = Paths.get(jniTempPath.toString(), "objects", osName, System.getProperty("os.arch")).toString()
                env["CFLAGS"] = "-O2 -fomit-frame-pointer -Werror -Wall -fPIC -flto"

                exec {
                    executable = "make"
                    args = listOf("clean", "all")
                    environment = env
                }
            }

            Os.isFamily(Os.FAMILY_WINDOWS) -> {
                if (System.getenv("MSVC") != null) {
                    exec {
                        executable = "cmd"
                        args = listOf("/C", "windows_build.bat")
                    }
                } else {
                    if (System.getenv("CC") == null) env["CC"] = "gcc"
                    env["DYLIB_SUFFIX"] = "dll"
                    env["JNI_PLATFORM"] = "win32"
                    env["LIB_DIR"] = Paths.get(jniTempPath.toString(), "compiled", "windows", System.getProperty("os.arch")).toString()
                    env["OBJ_DIR"] = Paths.get(jniTempPath.toString(), "objects", "windows", System.getProperty("os.arch")).toString()
                    env["CFLAGS"] = "-O2 -fomit-frame-pointer -Werror -Wall -fPIC -flto"

                    exec {
                        executable = "make"
                        args = listOf("clean", "all")
                        environment = env
                    }
                }
            }

            else -> throw RuntimeException("Your OS isn't supported.")
        }
    }
}

sourceSets {
    if (!project.hasProperty("only_interface") && System.getenv("ci") != null) {
        main {
            resources.srcDir(Paths.get(project.rootDir.toString(), "tmp", "compiled"))
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("compileNatives")
}

tasks.named<Test>("test") {
    dependsOn("compileNatives")
    useJUnitPlatform()
}

tasks.jar {
    dependsOn("compileNatives")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
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
