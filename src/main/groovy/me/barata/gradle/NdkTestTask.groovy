package me.barata.gradle

import com.android.build.gradle.AppExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*

import static me.barata.gradle.AdbHelper.*
import static me.barata.gradle.CommandHelper.*

public class NdkTestTask extends DefaultTask implements VerificationTask {
	private static final String DEVICE_TEMP_FOLDER = '/data/local/tmp/';
	private static final String TEST_REPORT_FILE = 'testReport.xml'

	private final AppExtension android;

	public NdkTestTask() {
		android = project.extensions.getByType(AppExtension.class);
	}

	@InputDirectory
	File getNdkOutputDirectory() {
		return ndkTestCompileTask.ndkOutputDirectory;
	}

	@InputDirectory
	File getNdkTestOutputDirectory() {
		return ndkTestCompileTask.outputFolder;
	}

	@InputFiles
	Set<File> getAssetDirectories() {
		Set<File> testSrcDir = android.sourceSets.test.assets.srcDirs;
		if (testSrcDir != null) {
			for (File file: testSrcDir) {
				if (file.exists()) {
					return testSrcDir;
				}
			}
		}
		// either not specified or no test assets directories exist

		return android.sourceSets.main.assets.srcDirs;
	}

	@OutputFile
	File getTestReport() {
		return new File(testReportDirectory, TEST_REPORT_FILE);
	}

	@OutputFile
	File getTestLog() {
		return new File(testReportDirectory, 'testLog.txt');
	}

	private NdkTestCompileTask ndkTestCompileTask;
	private boolean ignoreFailures;

	public NdkTestCompileTask getNdkTestCompileTask() {
		return ndkTestCompileTask;
	}

	public void setNdkTestCompileTask(NdkTestCompileTask ndkTestCompileTask) {
		this.ndkTestCompileTask = ndkTestCompileTask;
	}

	File getTestReportDirectory() {
		return new File(project.buildDir.absolutePath + '/reports/ndkTest/');
	}

	@TaskAction
	void taskAction() {
//		boolean jniDebuggable = ndkTestCompileTask.debuggable;

		List<Device> devices = listDevices();
		if (devices.size() != 1) {
			project.logger.error("Could not execute because there's either no or more than one ready device: " + devices);
			return;
		}

		Device device = devices.get(0);
		if (device.architecture == null) {
			project.logger.error("Could not find architecture of device using adb shell getprop ro.product.cpu.abilist." +
					"Please raise an issue.");
			return;
		}
		project.logger.info("Architecture found: " + device.architecture);

		project.logger.info("Copying assets to device in relative path: assets/");
		for (File assetDirectory : getAssetDirectories()) {
			project.logger.info("Copying folder: "+assetDirectory.absolutePath);
			copyFolder(device, assetDirectory, DEVICE_TEMP_FOLDER + 'assets/');
		}

		String ndkModulePath = ndkTestCompileTask.ndkCompiledModule.replace('$(TARGET_ARCH_ABI)', device.architecture.name());

		String ndkTestOutputArchitecturePath = ndkTestOutputDirectory.absolutePath + '/' + device.architecture + '/';
		String ndkTestModulePath = ndkTestOutputArchitecturePath + ndkTestCompileTask.testModuleName;
		String testExecutable = DEVICE_TEMP_FOLDER + ndkTestCompileTask.testModuleName;

		project.logger.info('Copying test executable to device');
		runWithOutput(adb(device, 'push', ndkModulePath, DEVICE_TEMP_FOLDER));
		runWithOutput(adb(device, 'push', ndkTestModulePath, DEVICE_TEMP_FOLDER));

		// TODO: gdbserver not supported
//		if (jniDebuggable) {
		// push gdb server to device
//			run(adb(device, 'push', ndkTestOutputArchitecturePath + 'gdbserver', DEVICE_TEMP_FOLDER));
//			run(adb(device, 'push', ndkTestOutputArchitecturePath + 'gdb.setup', DEVICE_TEMP_FOLDER));
//			runWithLoggerError(adb(device, 'shell', 'chmod', '755', DEVICE_TEMP_FOLDER + 'gdbserver'));
//		}

		project.logger.info("Running tests...");
		runTests(device, testExecutable);
	}

	private void runTests(Device device, String testExecutable) {
		runWithLoggerError(adb(device, 'shell', 'chmod', '755', testExecutable));

		String remoteTestReportPath = DEVICE_TEMP_FOLDER + TEST_REPORT_FILE;

		List<String> cmd;
		// TODO: gdbserver not supported
//		if (ndkTestCompileTask.debuggable) {
//			cmd = adb(device,
//					'shell',
//					DEVICE_TEMP_FOLDER+'gdbserver \"LD_LIBRARY_PATH=' + DEVICE_TEMP_FOLDER + ' ' + testExecutable + "\"",
//					'--gtest_output=xml:' + remoteTestReportPath,
//					'--gtest_break_on_failure')
//		} else {
		cmd = adb(device,
				'shell',
				'\"LD_LIBRARY_PATH=' + DEVICE_TEMP_FOLDER + ' ' + testExecutable + "\"",
				'--gtest_output=xml:' + remoteTestReportPath)
//		}

		runWithInterpretor(
				cmd,
				logger,
				"ERROR"
		);

		testReport.delete();

		runWithOutput(adb(device, 'pull', remoteTestReportPath, testReportDirectory.absolutePath));
		runWithLoggerError(adb(device, 'shell', 'rm', remoteTestReportPath));

		if (!testReport.exists()) {
			throw new GradleException("Execution interrupted while testing");
		}

		logger.lifecycle('\nTest results: file://'+testReport.absolutePath);
	}

	private void runWithLoggerError(List<String> cmd) {
		runWithError(cmd, project.logger, getName());
	}

	@Override
	void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
	}

	@Override
	boolean getIgnoreFailures() {
		return ignoreFailures;
	}
}
