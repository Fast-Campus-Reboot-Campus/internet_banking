plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "com.bank"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
