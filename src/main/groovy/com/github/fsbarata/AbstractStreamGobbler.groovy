package com.github.fsbarata

abstract class AbstractStreamGobbler extends Thread {
	private final InputStream is;

	AbstractStreamGobbler(InputStream is) {
		this.is = is;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				gobble(line.trim());
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	abstract protected void gobble(String line);
}
