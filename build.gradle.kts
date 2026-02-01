import org.gradle.api.tasks.JavaExec

plugins {
    id("java")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "pt.training.maven.jee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


javafx {
    version = "21.0.7"
    modules("javafx.controls", "javafx.graphics")
}

dependencies {
    
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("com.h2database:h2")

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

    mainClass.set("pt.training.go.server.GoServer")
    
    standardInput = System.`in`
}

tasks.register<JavaExec>("runClient") {
    group = ".run"
    description = "Uruchamia client-a"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("pt.training.go.client.GoClient")

    args("localhost")

    standardInput = System.`in`
}

tasks.register<JavaExec>("runSmartBot") {
    group = ".run"
    description = "Uruchamia inteligentnego bota"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("pt.training.go.client.SmartBot")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runGuiClient") {
    group = ".run"
    description = "Uruchamia GUI client-a"

    val runtimeCp = configurations.runtimeClasspath.get()

    val javafxModulePath = runtimeCp.filter { it.name.startsWith("javafx-") }

    classpath = sourceSets["main"].runtimeClasspath.filter { !it.name.startsWith("javafx-") }

    mainClass.set("pt.training.go.client.gui.app.GuiClientApp")

    jvmArgs(
        "--module-path", javafxModulePath.asPath,
        "--add-modules", javafx.modules.joinToString(",")
    )
}