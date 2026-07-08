import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import me.modmuss50.mpp.ReleaseType
import java.io.FileNotFoundException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    id("fabric-loom") version "1.8-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.8.1"
    id("maven-publish")
    id("signing")
}

val ENV = System.getenv()
val CHANGELOG = if (File("CHANGELOG.md").exists()) File("CHANGELOG.md").readText() else ""

version = "${property("twitch4j")}+${property("mod_version")}"
group = property("maven_group") as String

val versions: MutableMap<String, String> = HashMap()

fun DependencyHandlerScope.includeVersion(dependency: String): Dependency? {
    val key = dependency.split(":", limit = 2)[0]
    if (!versions.containsKey(key)) throw NoSuchElementException("No version found for $key")
    return this.include("$dependency:${versions[key]}")
}

fun DependencyHandlerScope.setupVersion(dependency: String): String {
    val key = dependency.split(":", limit = 2)[0]
    val version = dependency.substring(dependency.lastIndexOf(":"))
    versions[key] = version

    return dependency
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    /* ===== Twitch4J ===== */
    include(api(setupVersion("com.github.twitch4j:twitch4j:${property("twitch4j")}")) {
        exclude("org.slf4j", "slf4j-api")
    })
    includeVersion("com.github.twitch4j:twitch4j-auth")
    includeVersion("com.github.twitch4j:twitch4j-chat")
    includeVersion("com.github.twitch4j:twitch4j-client-websocket")
    includeVersion("com.github.twitch4j:twitch4j-common")
    includeVersion("com.github.twitch4j:twitch4j-eventsub-common")
    includeVersion("com.github.twitch4j:twitch4j-eventsub-websocket")
    includeVersion("com.github.twitch4j:twitch4j-extensions")
    includeVersion("com.github.twitch4j:twitch4j-graphql")
    includeVersion("com.github.twitch4j:twitch4j-helix")
    includeVersion("com.github.twitch4j:twitch4j-kraken")
    includeVersion("com.github.twitch4j:twitch4j-messaginginterface")
    includeVersion("com.github.twitch4j:twitch4j-pubsub")
    includeVersion("com.github.twitch4j:twitch4j-util")

    /* ===== Events4J ===== */
    platform("com.github.philippheuer.events4j:events4j-bom:${property("events4j")}")
    include("com.github.philippheuer.events4j:events4j-api")
    include("com.github.philippheuer.events4j:events4j-core")
    include("com.github.philippheuer.events4j:events4j-handler-simple")

    include("com.github.philippheuer.credentialmanager:credentialmanager:0.4.0")

    /* ===== Xanthic ===== */
    platform("io.github.xanthic.cache:cache-bom:${property("xanthic")}")
    include("io.github.xanthic.cache:cache-api")
    include("io.github.xanthic.cache:cache-core")
    include("io.github.xanthic.cache:cache-provider-caffeine")

    /* ===== Apollo ===== */
    platform("com.apollographql.apollo:apollo-bom:${property("apollo")}")
    include("com.apollographql.apollo:apollo-api-jvm")
    include("com.apollographql.apollo:apollo-http-cache-api")
    include("com.apollographql.apollo:apollo-normalized-cache-api-jvm")
    include("com.apollographql.apollo:apollo-normalized-cache-jvm")
    include("com.apollographql.apollo:apollo-runtime")

    /* ===== Jackson ===== */
    platform("com.fasterxml.jackson.core:jackson-bom:${property("jackson")}")
    include("com.fasterxml.jackson.core:jackson-annotations")
    include("com.fasterxml.jackson.core:jackson-core")
    include("com.fasterxml.jackson.core:jackson-databind")
    include("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    /* ===== Feign ===== */
    platform("io.github.openfeign:feign-bom:${property("openfeign")}")
    include("io.github.openfeign:feign-core")
    include("io.github.openfeign:feign-hystrix")
    include("io.github.openfeign:feign-jackson")
    include("io.github.openfeign:feign-okhttp")
    include("io.github.openfeign:feign-slf4j")

    // Other dependencies
    include("com.benasher44:uuid-jvm:0.2.0")
    include("com.bucket4j:bucket4j_jdk8-core:8.10.1")
    include("com.github.ben-manes.caffeine:caffeine:2.9.3")
    include("com.github.tony19:named-regexp:1.0.0")
    include("com.google.errorprone:error_prone_annotations:2.10.0")
    include("com.neovisionaries:nv-websocket-client:2.14")
    include("com.netflix.archaius:archaius-core:0.4.1")
    include("com.netflix.hystrix:hystrix-core:1.5.18")
    include("com.nytimes.android:cache:2.0.2")
    include("com.squareup.okhttp3:okhttp:4.12.0")
    include("com.squareup.okio:okio-jvm:3.6.0")
    include("io.reactivex:rxjava:1.2.0")
    include("org.hdrhistogram:HdrHistogram:2.1.9")

    // Non-standard namespaces
    include("commons-configuration:commons-configuration:1.10")
    include("commons-lang:commons-lang:2.6")

    // Minecraft 1.21.11 has JSpecify, but it's fine to include it
    include("org.jspecify:jspecify:1.0.0")
}

tasks.processResources {
    val map = mapOf(
        "version" to property("version")
    )

    inputs.properties(map)

    filesMatching("fabric.mod.json") {
        expand(map)
    }
}

val targetJavaVersion = 8
tasks.withType<JavaCompile> {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release = targetJavaVersion
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${rootProject.name}" }
    }
}

val emptyJavadocJar = tasks.register<Jar>("emptyJavadocJar") {
    archiveClassifier.set("javadoc")
}

val emptySourcesJar = tasks.register<Jar>("emptySourcesJar") {
    archiveClassifier.set("sources")
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = rootProject.name
            from(components.getByName("java"))

            artifact(emptySourcesJar) {
                classifier = "sources"
                builtBy(emptySourcesJar)
            }

            artifact(emptyJavadocJar) {
                classifier = "javadoc"
                builtBy(emptyJavadocJar)
            }

            pom {
                name = "twitch4j-fabric"
                description = "Twitch4J packed as a Fabric Mod"
                url = "https://twitch4j.github.io"

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/twitch4j/twitch4j-fabric/issues"
                }

                licenses {
                    license {
                        name = "MIT Licence"
                        distribution = "repo"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "PhilippHeuer"
                        name = "Philipp Heuer"
                        email = "git@philippheuer.me"
                        roles = listOf("maintainer")
                    }

                    developer {
                        id = "iProdigy"
                        name = "Sidd"
                        roles = listOf("maintainer")
                    }

                    developer {
                        id = "AwakenedRedstone"
                        name = "Awakened Redstone"
                        roles = listOf("maintainer")
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/twitch4j/twitch4j-fabric.git"
                    developerConnection = "scm:git:git@github.com:twitch4j/twitch4j-fabric.git"
                    url = "https://github.com/twitch4j/twitch4j-fabric"
                }
            }
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        //mavenLocal()
        maven {
            // Fallback if not set to avoid NPE on sync
            setUrl(project.findProperty("mavenRepoUrl") ?: "")

            credentials {
                username = project.findProperty("mavenRepoUsername") as String?
                password = project.findProperty("mavenRepoPassword") as String?
            }
        }
    }
}

tasks.named("generateMetadataFileForMavenJavaPublication") {
    dependsOn(tasks.named("emptySourcesJar"), tasks.named("emptyJavadocJar"))
}

val signingKey = project.findProperty("signingKey")
val signingPassword = project.findProperty("signingPassword")

signing {
    // use in-memory signing
    if (signingKey != null) {
        val decodedSigningKey = String(Base64.getDecoder().decode(signingKey.toString()), StandardCharsets.UTF_8)
        useInMemoryPgpKeys(decodedSigningKey, signingPassword as String)
    } else {
        isRequired = false // only sign when credentials are configured
        if (!project.hasProperty("gnupg.skip")) {
            useGpgCmd()
        }
    }

    sign(publishing.publications.getByName("mavenJava"))
}

val checkVersion: TaskProvider<Task> = tasks.register("checkVersion") {
    fun Node.chain(name: String): Node {
        return this.get(name) as Node
    }

    fun Node.list(name: String): List<Node> {
        return (this.get(name) as NodeList).mapNotNull { it as Node }
    }

    doFirst {
        try {
            val xml = URI("https://repo1.maven.org/maven2/com/github/twitch4j/twitch4j-fabric/maven-metadata.xml").toURL().readText()
            val metadata = XmlParser().parseText(xml)
            val versions = metadata.chain("versioning").chain("versions").list("version").map { it }
            if (versions.contains(version)) {
                throw RuntimeException("$version has already been released!")
            }
        } catch (_: FileNotFoundException) {
            // 404 or 410, consider as no version has been released
        }
    }
}

tasks.publish.get().dependsOn(checkVersion)

publishMods {
    val (versionName, versionType) = getVersionData()
    displayName = versionName
    type = versionType
    // Not using the remapped JAR since there is nothing to remap
    file = tasks.jar.get().archiveFile
    modLoaders.add("fabric")
    modLoaders.add("quilt")
    changelog = "### The Twitch4J changelog can be seen at https://github.com/twitch4j/twitch4j/releases/tag/v${property("twitch4j")}\n\n" + CHANGELOG
    dryRun = ENV["GITHUB_TOKEN"] == null

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "CmHD69Pj"
        requires("fabric-language-kotlin")
        minecraftVersionRange {
            start = "1.14"
            end = "latest"
        }
    }

    github {
        accessToken = providers.environmentVariable("GITHUB_TOKEN")
        repository = if (ENV["CI"] != null) providers.environmentVariable("GITHUB_REPOSITORY") else providers.provider { "FabricMC/dryrun" }
        commitish = if (ENV["CI"] != null) getBranch() else "dryrun"
    }
}

fun getBranch(): String {
    val branch = ENV["GITHUB_REF"]
    if (branch != null) {
        return branch.substring(branch.lastIndexOf("/") + 1)
    }

    throw RuntimeException("Unable to get branch")
}

fun getVersionData(): Pair<String, ReleaseType> {
    val projectVersion: String = property("mod_version").toString()
    val projectVersionNumber: List<String> = projectVersion.split(Regex("-"), 2)
    var projectVersionName = "Release ${projectVersionNumber[0]}"
    var projectVersionType = ReleaseType.STABLE
    if (projectVersion.contains("beta")) {
        val projectBeta: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Beta ${projectBeta[1]}"
        projectVersionType = ReleaseType.BETA
    } else if (projectVersion.contains("alpha")) {
        val projectAlpha: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Alpha ${projectAlpha[1]}"
        projectVersionType = ReleaseType.ALPHA
    } else if (projectVersion.contains("rc")) {
        val projectRC: List<String> = projectVersionNumber[1].split(Regex("\\."), 2)
        projectVersionName = "${projectVersionNumber[0]} - Release Candidate ${projectRC[1]}"
        projectVersionType = ReleaseType.BETA //Modrinth doesn't have RC so I use beta
    }

    return Pair(projectVersionName, projectVersionType)
}
