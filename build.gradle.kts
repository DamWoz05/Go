import org.gradle.api.tasks.JavaExec

plugins {
    id("java")
}

group = "pt.training.maven.jee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

        tasks.register<JavaExec>("runServer") {
            group = ".run"
            description = "Uruchamia server"

            classpath = sourceSets["main"].runtimeClasspath

            mainClass.set("pt.training.go.GoServer")
        }

tasks.register<JavaExec>("runClient") {
    group = ".run"
    description = "Uruchamia client-a"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("pt.training.go.GoClient")

    args("localhost")

    standardInput = System.`in`
}
