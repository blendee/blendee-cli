package org.blendee.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

public class Params {

	boolean regenerate;

	String[] schemaNames;

	String packageName;

	Path output;

	String encoding;

	String url;

	String username;

	String password;

	Properties options;

	String[] tables;

	boolean verbose;

	private static final String newLine = System.getProperty("line.separator");

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("parameters" + newLine);
		builder.append("  regenerate: " + regenerate + newLine);
		builder.append("  schemaNames: [" + String.join(", ", schemaNames) + "]" + newLine);
		builder.append("  packageName: " + packageName + newLine);
		builder.append("  output: " + output + newLine);
		builder.append("  encoding: " + encoding + newLine);
		builder.append("  url: " + url + newLine);
		builder.append("  username: " + username + newLine);
		builder.append("  password: " + String.join("", Collections.nCopies(password.length(), "*")) + newLine);
		builder.append("  options: " + options + newLine);
		builder.append("  tables: [" + String.join(", ", tables) + "]" + newLine);
		builder.append("  verbose: " + verbose);

		return builder.toString();
	}
}
