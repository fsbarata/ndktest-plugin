package me.barata.gradle

import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

import java.nio.charset.StandardCharsets

public class CommandHelper {
	public static List<String> runWithOutput(List<String> cmd) {
		return runWithPrinter(cmd, null, null, null, null);
	}

	public static List<String> runWithPrinter(List<String> cmd, PrintStream outputStream, PrintStream errorStream) {
		return runWithPrinter(cmd, outputStream, errorStream, null, null);
	}

	public
	static List<String> runWithPrinter(List<String> cmd, PrintStream outputStream, PrintStream errorStream, String tag) {
		return runWithPrinter(cmd, outputStream, errorStream, tag, tag);
	}

	public
	static List<String> runWithPrinter(List<String> cmd, PrintStream outputStream, PrintStream errorStream, String outTag, String errorTag) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream stringPrinter = new PrintStream(baos);

		Process process = new ProcessBuilder(cmd).start();

		StreamGobbler outputGobbler, errorGobbler;
		if (outputStream != null) {
			outputGobbler = new StreamGobbler(process.getInputStream(), outTag, stringPrinter, outputStream);
		} else {
			outputGobbler = new StreamGobbler(process.getInputStream(), outTag, stringPrinter);
		}
		if (errorStream != null) {
			errorGobbler = new StreamGobbler(process.getErrorStream(), errorTag, stringPrinter, errorStream);
		} else {
			errorGobbler = new StreamGobbler(process.getErrorStream(), errorTag, stringPrinter);
		}

		gobbleProcess(process, outputGobbler, errorGobbler);

		List<String> output = new LinkedList<>();

		String[] lines = baos.toString(StandardCharsets.UTF_8.name()).replace("\r", "").split("\n");
		for (String line : lines) {
			if (line.startsWith('*')) {
				continue;
			}

			output.add(line);
		}

		return output;
	}

	public static int run(List<String> cmd) {
		Process process = new ProcessBuilder(cmd).start();
		return process.waitFor();
	}

	public static int runWithLogger(List<String> cmd, Logger logger, String outputTag, String errorTag) {
		Process process = new ProcessBuilder(cmd).start();
		LoggerGobbler outputGobbler = new LoggerGobbler(process.getInputStream(), outputTag, logger, LogLevel.INFO);
		LoggerGobbler errorGobbler = new LoggerGobbler(process.getErrorStream(), errorTag, logger, LogLevel.ERROR);
		return gobbleProcess(process, outputGobbler, errorGobbler);
	}

	public static int runWithError(List<String> cmd, Logger logger, String tag) {
		Process process = new ProcessBuilder(cmd).start();
		LoggerGobbler errorGobbler = new LoggerGobbler(process.getErrorStream(), tag, logger, LogLevel.ERROR);
		return gobbleProcess(process, null, errorGobbler);
	}

	public static int runWithInterpretor(List<String> cmd, Logger logger, String errorTag) {
		PrintStream errorStream = LoggerStream.createPrintStream(logger, LogLevel.ERROR);

		Process process = new ProcessBuilder(cmd).start();

		GoogleTestInterpretor interpretor = new GoogleTestInterpretor(process.getInputStream(), logger);
		StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), errorTag, errorStream);

		int resCode = gobbleProcess(process, interpretor, errorGobbler);

		if (interpretor.failure) {
			throw new GradleException('Failed to run tests.');
		}

		return resCode;
	}

	private
	static int gobbleProcess(Process process, AbstractStreamGobbler outputGobbler, AbstractStreamGobbler errorGobbler) {
		if (outputGobbler != null) {
			outputGobbler.start();
		}
		if (errorGobbler != null) {
			errorGobbler.start();
		}
		int resCode = process.waitFor();
		if (outputGobbler != null) {
			outputGobbler.join();
		}
		if (errorGobbler != null) {
			errorGobbler.join();
		}
		return resCode;
	}
}
