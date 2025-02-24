plugins {
	kotlin("jvm") version libs.versions.kotlin;
	`java-library`;
	`maven-publish`;
	signing;
};

group = "codes.rorak";
version = "1.1.7-1-SNAPSHOT";
description = "A very simple and easy-to-use Ktor folder routing plugin.";

repositories {
	mavenCentral();
};

dependencies {
	api(libs.ktor.core);
	
	compileOnly(libs.ktor.plugin.statusPages);
	compileOnly(libs.ktor.plugin.websockets);
	compileOnly(libs.ktor.plugin.auth);
	
	implementation(libs.reflections);
	
	testImplementation(kotlin("test"));
	testImplementation(libs.ktor.test);
	testImplementation(libs.ktor.engine.netty);
	testImplementation(libs.ktor.plugin.statusPages);
	testImplementation(libs.ktor.plugin.websockets);
	testImplementation(libs.ktor.plugin.auth);
};

tasks.test {
	useJUnitPlatform();
};

kotlin {
	jvmToolchain(17);
};

java {
	withSourcesJar();
	withJavadocJar();
};

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"]);
			
			pom {
				name = "Better Ktor";
				description = project.description;
				url = "https://github.com/RORAKUS/betterktor";
				packaging = "jar";
				developers {
					developer {
						id = "rorakus";
						name = "RORAK";
						email = "rorakcodes@gmail.com";
					};
				};
				licenses {
					license {
						name = "GNU General Public License v3.0";
						url = "https://opensource.org/license/gpl-3-0";
						distribution = "repo";
					};
				};
				scm {
					connection = "scm:git:https://github.com/RORAKUS/betterktor.git";
					developerConnection = "scm:git:https://github.com/RORAKUS/betterktor.git";
					url = "https://github.com/RORAKUS/betterktor";
				};
			};
		};
	};
	repositories {
		maven {
			name = "OSSHR";
			url = uri(
				if (version.toString().endsWith("SNAPSHOT"))
					"https://s01.oss.sonatype.org/content/repositories/snapshots/"
				else
					"https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
			);
			credentials {
				username = System.getenv("OSSRH_USERNAME");
				password = System.getenv("OSSRH_PASSWORD");
			};
		};
	};
};

signing {
	useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSPHRASE"));
	sign(publishing.publications["maven"]);
};