plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("io.javalin:javalin:3.7.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
}
