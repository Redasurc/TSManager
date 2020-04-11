plugins {
	base
	kotlin("jvm") version "1.3.61" apply false
}

allprojects {

	group = "eu.redasurc.tsm"
	version = "0.0.4-SNAPSHOT"
	repositories {
		jcenter()
		maven (
				url = "https://jitpack.io"
		)
	}
}

dependencies {
	// Make the root project archives configuration depend on every subproject
	subprojects.forEach {
		archives(it)
	}
}