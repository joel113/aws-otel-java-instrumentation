/*
 * Copyright Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jk1.license.LicenseReportExtension

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.12.1",
  "com.google.guava:guava-bom:30.1-jre",
  "com.google.protobuf:protobuf-bom:3.14.0",
  "com.linecorp.armeria:armeria-bom:1.4.0",
  "io.grpc:grpc-bom:1.35.0",
  "io.opentelemetry:opentelemetry-bom:0.15.0",
  "org.apache.logging.log4j:log4j-bom:2.14.0",
  "org.junit:junit-bom:5.7.0",
  "org.springframework.boot:spring-boot-dependencies:2.4.2",
  "org.testcontainers:testcontainers-bom:1.15.1",
  "software.amazon.awssdk:bom:2.15.73"
)

val DEPENDENCY_SETS = listOf(
  DependencySet(
    "io.opentelemetry",
    "0.15.0-alpha",
    listOf(
      "opentelemetry-api-metrics",
      "opentelemetry-sdk-extension-autoconfigure"
    )
  ),
  DependencySet(
    "io.opentelemetry.javaagent",
    "0.15.0",
    listOf(
      "opentelemetry-javaagent",
      "opentelemetry-javaagent-spi"
    )
  ),
  DependencySet(
    "org.assertj",
    "3.19.0",
    listOf("assertj-core")
  ),
  DependencySet(
    "org.curioswitch.curiostack",
    "1.2.0",
    listOf("protobuf-jackson")
  ),
  DependencySet(
    "org.slf4j",
    "1.7.30",
    listOf(
      "slf4j-api",
      "slf4j-simple"
    )
  )
)

val DEPENDENCIES = listOf(
  "commons-logging:commons-logging:1.2",
  "com.sparkjava:spark-core:2.9.3",
  "com.squareup.okhttp3:okhttp:4.9.1"
)

javaPlatform {
  allowDependencies()
}

dependencies {
  for (bom in DEPENDENCY_BOMS) {
    api(platform(bom))
  }
  constraints {
    for (set in DEPENDENCY_SETS) {
      for (module in set.modules) {
        api("${set.group}:$module:${set.version}")
      }
    }

    for (dependency in DEPENDENCIES) {
      api(dependency)
    }
  }
}

rootProject.allprojects {
  plugins.withId("com.github.jk1.dependency-license-report") {
    configure<LicenseReportExtension> {
      val bomExcludes = DEPENDENCY_BOMS.stream()
        .map { it.substring(0, it.lastIndexOf(':')) }
        .toArray { length -> arrayOfNulls<String>(length) }
      excludes = bomExcludes
    }
  }
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isGuava = version.endsWith("-jre")
  val isStable = stableKeyword || regex.matches(version) || isGuava
  return isStable.not()
}

tasks {
  named<DependencyUpdatesTask>("dependencyUpdates") {
    revision = "release"
    checkConstraints = true

    rejectVersionIf {
      isNonStable(candidate.version)
    }
  }
}
