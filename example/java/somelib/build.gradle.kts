plugins { java }
group = "dev.diplomattest"
version = "1.0-SNAPSHOT"
repositories { mavenCentral() }
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
