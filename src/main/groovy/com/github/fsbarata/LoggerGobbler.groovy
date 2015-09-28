package com.github.fsbarata

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

class LoggerGobbler extends AbstractStreamGobbler {
	private final String type;
	private final Logger logger;
	private final LogLevel logLevel;

	LoggerGobbler(InputStream is, String type, Logger logger, LogLevel level) {
		super(is);
		this.type = type;
		this.logger = logger;
		this.logLevel = level;
	}

	@Override
	protected void gobble(String line) {
		String toWrite;
		if (line.isEmpty()) {
			return;
		}
		if (type != null) {
			toWrite = type + ": " + line;
		} else {
			toWrite = line;
		}

		logger.log(logLevel, toWrite);
	}
}
