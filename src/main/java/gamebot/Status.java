package gamebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.io.Files;

public class Status {

	private static List<String> statusChecks;

	public static void init() {
		try {
			statusChecks = Files.readLines(new File("statuses"), Charset.defaultCharset());
		} catch (Exception e) {
			ChannelLogger.logMessage("Failed to read statuses file");
		}
	}

	public static String getStatus() {
		String cpuUsage = "**CPU Status:**\n```"+ executeBash("cd && mpstat -P ALL 2 1") + "```\n";
		String memoryUsage = "**Memory Status:**\n```"+ executeBash("cd && free -h") + "```\n";
		StringBuilder statuses = new StringBuilder("**Hostname**: edinvg.ddns.net\n**Service Status:**\n");
		for (String status : statusChecks) {
			String[] data = status.split("\\s");
			String port = data[data.length - 1];
			boolean isUp = executeBash("cd && sudo lsof -i -P -n | grep " + port).length() > 3;
			statuses.append((isUp ? ":white_check_mark: " : ":x: ") + status.replace(port, "on Port " + port) + "\n");
		}
		return cpuUsage + memoryUsage + statuses.toString();
	}

	private static String executeBash(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("sh", "-c", command);
			builder.directory(new File(System.getProperty("user.home")));
			Process process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			process.waitFor();
			return sb.toString();
		} catch (IOException | InterruptedException e) {
			ChannelLogger.logMessage("Bash script failed to execute: " + e.getStackTrace()[0]);
			return ":x:";
		}
	}

}
