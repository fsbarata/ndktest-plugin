package com.github.fsbarata

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

class NdkTestPlugin implements Plugin<Project> {
	@Override
	void apply(Project project) {
		project.plugins.withId('com.android.application') {
			AppExtension android = project.extensions.getByType(AppExtension);

			TestNdkOptions testNdkOptions = project.extensions.create('ndktest', TestNdkOptions);
			android.applicationVariants.all { ApplicationVariant variant ->
				NdkTestCompileTask ndkTestCompileTask = (NdkTestCompileTask) project.task("compile${variant.name.capitalize()}NdkTest",
						type: NdkTestCompileTask,
						dependsOn: "compile${variant.name.capitalize()}Ndk",
				);
				ndkTestCompileTask.ndkCompileTask = variant.ndkCompile
				ndkTestCompileTask.testNdkOptions = testNdkOptions;
				ndkTestCompileTask.variant = variant;

				NdkTestTask ndkTestTask = (NdkTestTask) project.task("connected${variant.name.capitalize()}NdkTest",
						type: NdkTestTask,
						dependsOn: "compile${variant.name.capitalize()}NdkTest"
				);
				ndkTestTask.ndkTestCompileTask = ndkTestCompileTask;
				ndkTestTask.outputs.upToDateWhen { false }
			}
		}
	}
}
