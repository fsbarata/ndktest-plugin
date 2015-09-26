package me.barata.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

class LoggerStream extends OutputStream {
	private final static char LINE_BREAK = '\n';

	public final Logger logger;
	public final LogLevel level;
	public final String tag;

	private String buffer = "";

	LoggerStream(Logger logger, LogLevel level) {
		this(logger, level, null);
	}

	LoggerStream(Logger logger, LogLevel level, String tag) {
		this.logger = logger
		this.level = level
		this.tag = tag;
	}

	@Override
	void write(int b) throws IOException {
		char c = b;
		if (c == LINE_BREAK) {
			if (tag != null) {
				logger.log(level, "/" + tag + ": " + buffer.replace("\r", ""));
			} else {
				logger.log(level, buffer.replace("\r", ""));
			}
			buffer = "";
		} else {
			buffer += c;
		}
	}

	public static PrintStream createPrintStream(Logger logger, LogLevel level) {
		createPrintStream(logger, level, null);
	}

	public static PrintStream createPrintStream(Logger logger, LogLevel level, String tag) {
		return new PrintStream(new LoggerStream(logger, level, tag));
	}
}
