
plugins {
    `java-library`
    jacoco
}

group = "org.example"
version = "1.0-SNAPSHOT"
repositories {
    mavenCentral()
}
dependencies {
    implementation("jakarta.inject:jakarta.inject-api:2.0.1.MR")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation(project(":DiContainer"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:4.6.1")
    testImplementation("org.eclipse.jetty:jetty-server:11.0.9")
    testImplementation("org.eclipse.jetty:jetty-servlet:11.0.9")

}
tasks.withType<Test>() {
    useJUnitPlatform()
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}