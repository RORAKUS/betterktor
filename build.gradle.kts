import com.vanniktech.maven.publish.SonatypeHost

plugins {
	kotlin("jvm") version libs.versions.kotlin;
	alias(libs.plugins.mavenPublish);
	`java-library`;
	signing;
};

group = "codes.rorak";
version = "1.1.7";
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

mavenPublishing {
	publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL);
	signAllPublications();
	pom {
		name = "Better Ktor";
		description = project.description;
		url = "https://github.com/RORAKUS/betterktor";
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