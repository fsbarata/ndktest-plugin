package com.github.fsbarata

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class NdkTestPluginTest {
	private Project setup() {
		Project project = ProjectBuilder.builder().build()
		project.buildscript {
			repositories {
				jcenter()
				mavenLocal()
				mavenCentral()
			}

			dependencies {
				classpath 'com.github:ndktest-fsbarata:1.0.0'
				classpath 'com.android.tools.build:gradle-experimental:0.2.0'
			}
		}

		project.repositories {
			jcenter()
		}

		project.pluginManager.apply 'com.android.model.application'

		project.model {
			android {
				compileSdkVersion = 23
				buildToolsVersion = "23.0.1"

				defaultConfig.with {
					applicationId = "com.github.ndktest.app"
					minSdkVersion.apiLevel = 14
					targetSdkVersion.apiLevel = 23
				}
			}

			android.buildTypes {
				debug {
				}
				debugJni {
					ndk.with {
						debuggable = true
					}
				}
				release {
					minifyEnabled = true
					shrinkResources = true
				}
			}

			android.productFlavors {
				create("paid") {
				}
				create("free") {
					applicationId = "com.github.ndktest.freeapp"
				}
			}

			android.sources {
				androidTest {
					setRoot('src/androidTest')

				}
				test {
					jni.source.srcDir = ['src/test/jni']
				}
			}

			android.ndk {
				moduleName = "core"
				cppFlags += "-std=c++11 -fexceptions"
				ldLibs += "log"
				ldLibs += "android"
				stl = "gnustl_static"
			}
		}

		project.pluginManager.apply 'com.github.ndktest'

		return project;
	}

	@Test
	public void greeterPluginAddsGreetingTaskToProject() {
		Project project = setup();
		// Not complete
	}

}
