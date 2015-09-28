package com.github.fsbarata

import com.android.annotations.NonNull
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.CoreNdkOptions
import com.android.build.gradle.tasks.NdkCompile
import com.android.utils.FileUtils
import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.io.Files
import org.codehaus.groovy.GroovyException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.util.PatternSet

import static com.android.SdkConstants.CURRENT_PLATFORM
import static com.android.SdkConstants.PLATFORM_WINDOWS
import static com.github.fsbarata.CommandHelper.runWithLogger

class NdkTestCompileTask extends DefaultTask {
	TestNdkOptions testNdkOptions;

	private final AppExtension android;

	public NdkTestCompileTask() {
		android = project.extensions.getByType(AppExtension.class);
	}

	@OutputFile
	File getMakefile() {
		return new File(buildOutputDirectory, "Android.mk");
	}

	@OutputDirectory
	File getOutputFolder() {
		return new File(buildOutputDirectory + "libs/");
	}

	@Input
	@Optional
	String getcFlags() {
		return testNdkOptions?.cFlags;
	}

	@Input
	@Optional
	Set<String> getLdLibs() {
		return testNdkOptions?.ldLibs;
	}

	@Input
	@Optional
	Set<String> getAbiFilters() {
		return testNdkOptions?.abiFilters;
	}

	@Input
	@Optional
	boolean getDebuggable() {
		return ndkCompileTask.debuggable;
	}

	@InputDirectory
	File getNdkOutputDirectory() {
		return ndkCompileTask.soFolder;
	}

	@InputFiles
	Set<File> getTestSourceDirs() {
		return android.sourceSets.test.jni.srcDirs;
	}

	String getNdkCompiledModule() {
		return ndkOutputDirectory.getAbsolutePath() + '/$(TARGET_ARCH_ABI)/lib' + moduleName + '.so';
	}

	private ApplicationVariant variant;
	private NdkCompile ndkCompileTask;
	private String buildOutputDirectory;

	String getModuleName() {
		return ndkCompileTask.moduleName != null ? ndkCompileTask.moduleName : project.name;
	}

	String getTestModuleName() {
		return moduleName + "_ndktest";
	}

	NdkCompile getNdkCompileTask() {
		return ndkCompileTask;
	}

	void setNdkCompileTask(NdkCompile ndkCompileTask) {
		this.ndkCompileTask = ndkCompileTask;
	}

	public void setVariant(ApplicationVariant variant) {
		this.variant = variant;
		buildOutputDirectory = project.buildDir.getAbsolutePath() + "/intermediates/ndktest/" + variant.dirName + "/";
	}

	public ApplicationVariant getVariant() {
		return variant;
	}

	@TaskAction
	void taskAction(IncrementalTaskInputs inputs) {
		if (testSourceDirs == null) {
			project.logger.warn("Warning, test directory not set: \n " +
					"android {\n" +
					"    sourceSets {\n" +
					"        test {\n" +
					"            jni.srcDirs = [put your directory here]\n" +
					"        }\n" +
					"    }\n" +
					"}\n");
		}

		new File(buildOutputDirectory).mkdirs();

		FileTree sourceFileTree = getSource();
		Set<File> sourceFiles = sourceFileTree.matching(new PatternSet().exclude("**/*.h", "**/*.mk")).files;

		if (sourceFiles.isEmpty()) {
			makefile.delete();
			FileUtils.deleteFolder(getOutputFolder());
			return;
		}

		boolean generateMakefile = false;

		if (!inputs.isIncremental()) {
			project.logger.warn("Incremental execution is disabled.");
			generateMakefile = true
		} else {
			inputs.outOfDate {
				changed ->
					if (changed.added || changed.removed) {
						generateMakefile = true
					}
			}
			inputs.removed { change ->
				generateMakefile = true
			}
		}

		project.logger.info(getName() + ": Generating test NDK makefile.");
		if (generateMakefile) {
			writeMakefile(sourceFiles, makefile);
		}

		project.logger.info(getName() + ": Compiling NDK tests.");
		buildNdkTests(makefile);
		project.logger.info(getName() + ": Done.");
	}

	private FileTree getSource() {
		FileTree src;
		Set<File> sources = testSourceDirs;
		if (sources != null && !sources.isEmpty()) {
			src = getProject().files(new ArrayList<Object>(sources)).getAsFileTree()
			if (src != null) {
				return src;
			}
		}
		throw new GroovyException("Could not find sources. Please define android.sourceSets.test.jni.srcDirs");
	}


	private void writeMakefile(@NonNull Set<File> sourceFiles, @NonNull File makefile) {
		CoreNdkOptions coreNdkOptions = ndkCompileTask.getNdkConfig();

		StringBuilder sb = new StringBuilder();

		sb.append('LOCAL_PATH := $(call my-dir)\n' +
				'include \$(CLEAR_VARS)\n');

		sb.append('LOCAL_MODULE := ').append(moduleName).append('\n');
		sb.append("LOCAL_SRC_FILES := " + ndkCompiledModule + "\n");
		sb.append("include \$(PREBUILT_SHARED_LIBRARY)\n\n");

		sb.append('include \$(CLEAR_VARS)\n');
		sb.append('LOCAL_MODULE := ').append(testModuleName).append('\n');

		if (coreNdkOptions.cFlags != null || cFlags != null) {
			sb.append('LOCAL_CFLAGS := ');
			if (coreNdkOptions.cFlags != null) {
				sb.append(coreNdkOptions.cFlags)
				if (cFlags != null) {
					sb.append(" ").append(cFlags)
				}
			} else if (cFlags != null) {
				sb.append(cFlags)
			}
			sb.append('\n')
		}

		// To support debugging from Android Studio.
		sb.append("LOCAL_LDFLAGS := -Wl,--build-id\n")

		List<String> fullLdLibs = new ArrayList<>();
		if (ldLibs != null) {
			fullLdLibs.addAll(ldLibs);
		}
		if (ndkCompileTask.ndkRenderScriptMode) {
			fullLdLibs.add("dl");
			fullLdLibs.add("log");
			fullLdLibs.add("jnigraphics");
			fullLdLibs.add("RScpp_static");
		}

		if (!fullLdLibs.isEmpty()) {
			sb.append('LOCAL_LDLIBS := \\\n')
			for (String lib : fullLdLibs) {
				sb.append('\t-l').append(lib).append(' \\\n')
			}
			sb.append('\n')
		}

		sb.append('LOCAL_SRC_FILES := \\\n')
		for (File sourceFile : sourceFiles) {
			sb.append('\t').append(sourceFile.absolutePath).append(' \\\n')
		}
		sb.append('\n')

		for (File sourceFolder : ndkCompileTask.getSourceFolders()) {
			sb.append("LOCAL_C_INCLUDES += ${sourceFolder.absolutePath}\n")
		}
		for (File sourceFolder : testSourceDirs) {
			sb.append("LOCAL_C_INCLUDES += ${sourceFolder.absolutePath}\n")
		}

		if (ndkCompileTask.ndkRenderScriptMode) {
			sb.append('LOCAL_LDFLAGS += -L$(call host-path,$(TARGET_C_INCLUDES)/../lib/rs)\n')
		}

		sb.append("LOCAL_SHARED_LIBRARIES := " + moduleName + "\n" +
				"LOCAL_STATIC_LIBRARIES := googletest_main\n")

		sb.append('\ninclude \$(BUILD_EXECUTABLE)\n\n')

		sb.append('$(call import-module,third_party/googletest)\n')

		Files.write(sb.toString(), makefile, Charsets.UTF_8);
	}

	private void buildNdkTests(@NonNull File makefile) {
		File ndkDirectory = ndkCompileTask.ndkDirectory;

		CoreNdkOptions coreNdkOptions = ndkCompileTask.getNdkConfig();

		List<String> cmd = new LinkedList<>();

		String exe = ndkDirectory.absolutePath + File.separator + "ndk-build"
		if (CURRENT_PLATFORM == PLATFORM_WINDOWS && !ndkCompileTask.ndkCygwinMode) {
			exe += ".cmd"
		}
		cmd.add(exe);

		cmd.add("NDK_PROJECT_PATH=null")
		cmd.add("APP_BUILD_SCRIPT=" + makefile.absolutePath)

		cmd.add("APP_PLATFORM=" + android.getCompileSdkVersion())

		cmd.add("NDK_OUT=" + buildOutputDirectory + "obj/")

		cmd.add("NDK_LIBS_OUT=" + outputFolder)

		cmd.add("APP_OPTIM=release");

		if (debuggable) {
			cmd.add("NDK_DEBUG=1") // NOT SUPPORTED YET
		}

		cmd.add("APP_STL=" + coreNdkOptions.getStl())

		Set<String> abiFilters;
		abiFilters = getAbiFilters();
		if (abiFilters == null) {
			abiFilters = coreNdkOptions.abiFilters
		}
		if (abiFilters != null && !abiFilters.isEmpty()) {
			if (abiFilters.size() == 1) {
				cmd.add("APP_ABI=" + abiFilters.iterator().next())
			} else {
				Joiner joiner = Joiner.on(',').skipNulls()
				cmd.add("APP_ABI=" + joiner.join(abiFilters.iterator()))
			}
		} else {
			cmd.add("APP_ABI=all")
		}

		int resultCode = runWithLogger(cmd, project.logger, getName(), getName());
		if (resultCode != 0) {
			throw new GradleException("Compilation failed.");
		}
	}
}
