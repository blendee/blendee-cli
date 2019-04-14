package org.blendee.cli;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.blendee.util.Blendee;

public class CLI {

	private static Option helpOption = Option.builder("h")
		.longOpt("help")
		.build();

	public static void main(String[] args) throws Exception {
		execute(args);
	}

	private static void execute(String[] args) throws Exception {
		Options options = new Options();

		options.addOption(helpOption);

		options.addOption(
			Option.builder("v")
				.longOpt("verbose")
				.desc("Verbose Mode")
				.type(Boolean.class)
				.build());

		options.addOption(
			Option.builder("r")
				.longOpt("regenerate")
				.desc("Regenerate Existing Classes")
				.type(Boolean.class)
				.build());

		options.addOption(
			Option.builder("s")
				.longOpt("schemas")
				.hasArg()
				.required()
				.desc("Schema Names (Comma Separated)")
				.build());

		options.addOption(
			Option.builder("p")
				.longOpt("package")
				.hasArg()
				.required()
				.desc("Output Package Name")
				.build());

		options.addOption(
			Option.builder("o")
				.longOpt("output")
				.hasArg()
				.desc("Output Directory")
				.build());

		options.addOption(
			Option.builder("e")
				.longOpt("encoding")
				.desc("Source File Encoding")
				.build());

		options.addOption(
			Option.builder("c")
				.longOpt("credential")
				.desc("Credential Information File")
				.build());

		options.addOption(
			Option.builder("u")
				.longOpt("url")
				.hasArg()
				.desc("JDBC URL")
				.build());

		options.addOption(
			Option.builder("U")
				.longOpt("user")
				.hasArg()
				.desc("JDBC User Name")
				.build());

		options.addOption(
			Option.builder("P")
				.longOpt("pass")
				.hasArg()
				.desc("JDBC Password")
				.build());

		options.addOption(
			Option.builder("D")
				.hasArgs()
				.valueSeparator()
				.desc("option (-Dproperty=value)")
				.build());

		CommandLineParser parser = new DefaultParser();

		try {
			if (help(args)) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.setOptionComparator(null);
				formatter.printHelp("blendee-cli", options, true);
				return;
			}

			CommandLine commandLine = parser.parse(options, args);

			Path output;
			if (commandLine.hasOption("o")) {
				output = Paths.get(commandLine.getOptionValue("o"));
			} else {
				output = Paths.get("");
			}

			output = output.toAbsolutePath();

			Params params = new Params();

			if (commandLine.hasOption("c")) {
				Path path = Paths.get(commandLine.getOptionValue("c"));

				if (!Files.exists(path)) {
					System.err.println("Certification File " + path + " not found.");
					System.exit(1);
					return;
				}

				Properties props = new Properties();

				props.load(new InputStreamReader(Files.newInputStream(path)));

				params.url = props.getProperty("url");
				params.username = props.getProperty("username");
				params.password = props.getProperty("password");
			} else {
				params.url = commandLine.getOptionValue("u");

				if (commandLine.hasOption("U")) {
					params.username = commandLine.getOptionValue("U");
				}

				if (commandLine.hasOption("P")) {
					params.password = commandLine.getOptionValue("P");
				}
			}

			if (commandLine.hasOption("r")) {
				params.regenerate = true;
			}

			params.schemaNames = commandLine.getOptionValue("s").split(",");
			params.packageName = commandLine.getOptionValue("p");
			params.output = output;
			params.encoding = commandLine.getOptionValue("e");
			params.options = commandLine.hasOption("D") ? commandLine.getOptionProperties("D") : new Properties();
			params.tables = commandLine.getArgs();

			if (commandLine.hasOption("v")) {
				params.verbose = true;
			}

			Blendee.getEnvironment().setDefaultMetadataFactoryClass(CLIAnnotationMetadataFactory.class);

			new Command(params).execute();
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			System.exit(1);
			return;
		}
	}

	private static boolean help(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption(helpOption);
		return new DefaultParser().parse(options, args, true).hasOption(helpOption.getOpt());
	}
}
