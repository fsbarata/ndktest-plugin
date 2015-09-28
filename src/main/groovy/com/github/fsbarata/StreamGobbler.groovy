package com.github.fsbarata

class StreamGobbler extends AbstractStreamGobbler {
	private final String type;
	private final PrintStream[] outputStreams;

	StreamGobbler(InputStream is, String type, PrintStream... outputStreams) {
		super(is);
		this.type = type;
		this.outputStreams = outputStreams;
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

		for (PrintStream outputStream : outputStreams) {
			outputStream.println(toWrite);
		}
	}
}
