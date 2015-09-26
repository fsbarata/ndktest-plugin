package me.barata.gradle

import static me.barata.gradle.CommandHelper.runWithOutput

public class AdbHelper {
	public static enum Architecture {
		x86_64,
		x86,
		arm64_v8a,
		armeabi_v7a,
		armeabi,
		mips64,
		mips;

		public static Architecture parse(String s) {
			return valueOf(s.replace('-', '_'));
		}
	};

	public static class Device {
		public final String serial;
		public final Architecture architecture;

		private Device(String serial, Architecture architecture) {
			this.serial = serial;
			this.architecture = architecture;
		}

		public String toString() {
			return serial + "(" + architecture + ")";
		}
	}

	public static List<Architecture> getArchitectureList(String deviceName) {
		List<String> cmd = new LinkedList<>();

		cmd.add('adb');
		cmd.add('-s')
		cmd.add(deviceName);
		cmd.add('shell');
		cmd.add('getprop');
		cmd.add('ro.product.cpu.abilist');

		List<Architecture> output = new LinkedList<>();

		List<String> architectureLines = runWithOutput(cmd);
		if (architectureLines.isEmpty()) {
			return output;
		}

		String[] architecturesStrings = architectureLines.get(architectureLines.size() - 1).split(',');
		for (String architectureString : architecturesStrings) {
			Architecture architecture = Architecture.parse(architectureString);
			if (architecture == null) {
				continue;
			}

			output.add(architecture);
		}

		Collections.sort(output);

		return output;
	}

	public static Architecture getArchitecture(String deviceName) {
		List<String> cmd = new LinkedList<>();

		cmd.add('adb');
		cmd.add('-s')
		cmd.add(deviceName);
		cmd.add('shell');
		cmd.add('getprop');
		cmd.add('ro.product.cpu.abi');

		List<String> architectureLines = runWithOutput(cmd);

		String architectureString = architectureLines.get(architectureLines.size() - 1);
		Architecture architecture = Architecture.parse(architectureString);
		if (architecture != null) {
			return architecture;
		}

		List<Architecture> architectureList = getArchitectureList(deviceName);
		if (architectureList.isEmpty()) {
			return null;
		}
		return architectureList.get(0);
	}

	public static List<Device> listDevices() {
		List<String> lines = runWithOutput(adb(null, 'devices'));

		List<Device> devices = new ArrayList<>();
		for (String line : lines) {
			line = line.replaceAll(' +', ' ').replace('\t', ' ');
			if (line.toLowerCase().startsWith('list of')) {
				continue;
			}

			String[] deviceParams = line.split(' ');
			if (deviceParams.length < 2) {
				// ignore because I don't know what to do with this
				continue;
			}

			if (!deviceParams[1].equals('device')) {
				// ignore because it's not ready
				continue;
			}

			String deviceName = deviceParams[0];
			Architecture architectures = getArchitecture(deviceName);
			if (architectures == null) {
				continue;
			}

			Device device = new Device(deviceName, architectures);
			devices.add(device);
		}

		return devices;
	}

	public static List<String> adb(Device device, String action, String... args) {
		List<String> cmd = new LinkedList<>();

		cmd.add('adb');
		if (device != null) {
			cmd.add('-s');
			cmd.add(device.serial);
		}
		cmd.add(action);
		for (String arg : args) {
			cmd.add(arg);
		}

		return cmd;
	}

	public static String[] listPathContents(Device device, String path) {
		return runWithOutput(adb(device, 'shell', 'ls', path));
	}

	public static void copyFolder(Device device, File localDirectory, String remotePath) {
		if (!remotePath.endsWith('/')) {
			remotePath += '/';
		}

		runWithOutput(adb(device, 'shell', 'mkdir', remotePath));

		File[] localDirs = localDirectory.listFiles(new FileFilter() {
			@Override
			boolean accept(File file) {
				return file.isDirectory()
			}
		});

		for (File localDir : localDirs) {
			String partialPath = localDir.absolutePath.substring(localDirectory.absolutePath.length());
			if (partialPath.equals('.') || partialPath.equals('..')) {
				continue;
			}

			copyFolder(device, localDir, remotePath + partialPath);
		}

		File[] localFiles = localDirectory.listFiles(new FileFilter() {
			@Override
			boolean accept(File file) {
				return !file.isDirectory()
			}
		});

		for (File localFile : localFiles) {
			String fileName = localFile.absolutePath.substring(localDirectory.absolutePath.length()+1);
			String remoteFile = remotePath + '/' + fileName;
			String[] findFile = listPathContents(device, remoteFile);
			if (findFile.length == 1 && findFile[0].equals(remoteFile)) {
				// file already in remote, skip
				continue;
			}

			runWithOutput(adb(device, 'push', localFile.absolutePath, remotePath));
		}
	}
}
