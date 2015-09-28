package com.github.fsbarata

import org.gradle.api.logging.Logger

class GoogleTestInterpretor extends AbstractStreamGobbler {
	private final Logger logger;

	private String numberOfTests = 0;
	private int testCount = 0;
	private String bufferedExecutionException = null;
	private boolean failure = false;

	GoogleTestInterpretor(InputStream is, Logger logger) {
		super(is);
		this.logger = logger
	}

	public boolean getFailure() {
		return failure;
	}

	@Override
	protected void gobble(String line) {
		if (line.isEmpty()) {
			return;
		}

		if (bufferedExecutionException != null) {
			def matcher = (line =~ 'what..: (.*)');
			if (matcher.matches()) {
				String what = (String) matcher[0][1];
				String message = 'Execution threw a ' + bufferedExecutionException + ': ' + what;
				logger.error(message);
				failure = true;
				bufferedExecutionException = null;
				return;
			} else {
				String message = 'Execution threw a ' + bufferedExecutionException;
				logger.error(message);
				failure = true;
				bufferedExecutionException = null;
			}
		}


		def matcher1 = (line =~ '\\[=+\\] Running (\\d+).*\\d+.*');
		if (matcher1.matches()) {
			numberOfTests = (String) matcher1[0][1];
			testCount = 0;
			logger.lifecycle('Running ' + numberOfTests + ' tests.');
			return;
		}

		def matcher2 = (line =~ '\\[.*RUN.*\\] (.*)');
		if (matcher2.matches()) {
			testCount++;
			String testName = (String) matcher2[0][1];
			logger.lifecycle('Running test ' + testName + ' (' + testCount + ' of ' + numberOfTests + ')');
			return;
		}

		def matcher3 = (line =~ '\\[.*OK.*\\] (.*) \\((.*)\\)');
		if (matcher3.matches()) {
			String testName = (String) matcher3[0][1];
			String time = (String) matcher3[0][2];
			logger.lifecycle('Test ' + testName + ' completed in ' + time + '.');
			return;
		}

		def matcher4 = (line =~ '\\[.*FAIL.*\\] (.*) \\((.*)\\)');
		if (matcher4.matches()) {
			String testName = (String) matcher4[0][1];
			String time = (String) matcher4[0][2];
			logger.error('Test ' + testName + ' failed in ' + time + '.');
			failure = true;
			return;
		}

		def matcher5 = (line =~ 'Aborted');
		if (matcher5.matches()) {
			logger.error('Failure: aborted');
			failure = true;
			return;
		}

		def matcher6 = (line =~ '\\[-+\\].*');
		if (matcher6.matches()) {
			return;
		}

		def matcher7 = (line =~ '\\[ *PASSED *\\].*');
		if (matcher7.matches()) {
			return;
		}

		def matcher8 = (line =~ '(.+)\\:(\\d+)\\: Failure');
		if (matcher8.matches()) {
			String filename = (String) matcher8[0][1];
			String lineNumber = (String) matcher8[0][2];
			String message = filename + ':' + lineNumber+': Failed in line '+lineNumber;
			logger.error(message);
			failure = true;
			return;
		}

		def matcher9 = (line =~ '.*throwing.*instance of \'(.*)\'');
		if (matcher9.matches()) {
			String classname = (String) matcher9[0][1];
			bufferedExecutionException = classname;
			return;
		}

		def matcher10 = (line =~ 'Value of: (.*)');
		if (matcher10.matches()) {
			String actual = (String) matcher10[0][1];
			logger.error('Actual: '+actual);
			return;
		}

		def matcher11 = (line =~ 'Expected: (.*)');
		if (matcher11.matches()) {
			logger.error(line);
			return;
		}

		def matcher12 = (line =~ '\\[=+\\] (\\d+).*\\d+.*ran. \\((.*) total\\)');
		if (matcher12.matches()) {
			String numberOfTests = (String) matcher12[0][1];
			String time = (String) matcher12[0][2];
			logger.lifecycle('Ran ' + numberOfTests + ' tests in '+time+'.');
			return;
		}
	}
}
