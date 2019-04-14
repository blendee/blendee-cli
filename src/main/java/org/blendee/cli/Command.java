package org.blendee.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.blendee.assist.TableFacadePackageRule;
import org.blendee.codegen.CodeFormatter;
import org.blendee.codegen.TableFacadeGenerator;
import org.blendee.codegen.TableFacadeGeneratorHandler;
import org.blendee.jdbc.BlendeeManager;
import org.blendee.jdbc.OptionKey;
import org.blendee.jdbc.SystemOutLogger;
import org.blendee.jdbc.TablePath;
import org.blendee.jdbc.VoidLogger;
import org.blendee.util.Blendee;
import org.blendee.util.BlendeeConstants;

public class Command {

	private static final String TABLE_FACADE_SUPERCLASS = "table-facade-superclass";

	private static final String ROW_SUPERCLASS = "row-superclass";

	private static final String CODE_FORMATTER_CLASS = "code-formatter-class";

	private static final String USE_NUMBER_CLASS = "use-number-class";

	private static final String NOT_USE_NULL_GUARD = "not-use-null-guard";

	private static final String JDBC_DRIVER_CLASS = "jdbc-driver-class";

	private final Params params;

	public Command(Params params) {
		this.params = params;
	}

	public void execute()
		throws Exception {
		Map<OptionKey<?>, Object> init = new HashMap<>();

		init.put(BlendeeConstants.TABLE_FACADE_PACKAGE, params.packageName);
		init.put(BlendeeConstants.SCHEMA_NAMES, params.schemaNames);

		init.put(BlendeeConstants.JDBC_URL, params.url);
		init.put(BlendeeConstants.JDBC_USER, params.username);
		init.put(BlendeeConstants.JDBC_PASSWORD, params.password);

		{
			String className = params.options.getProperty(BlendeeConstants.TRANSACTION_FACTORY_CLASS.getKey());
			if (presents(className))
				init.put(BlendeeConstants.TRANSACTION_FACTORY_CLASS, Class.forName(className));
		}

		{
			String className = params.options.getProperty(BlendeeConstants.METADATA_FACTORY_CLASS.getKey());
			if (presents(className))
				init.put(BlendeeConstants.METADATA_FACTORY_CLASS, Class.forName(className));
		}

		init.put(BlendeeConstants.USE_METADATA_CACHE, true);

		Class<?> tableFacadeSuperclass;
		String tableFacadeSuperclassName = params.options.getProperty(TABLE_FACADE_SUPERCLASS);
		if (presents(tableFacadeSuperclassName)) {
			tableFacadeSuperclass = Class.forName(tableFacadeSuperclassName);//名称だけなので通常のload
		} else {
			tableFacadeSuperclass = null;
		}

		Class<?> rowSuperclass;
		String rowSuperclassName = params.options.getProperty(ROW_SUPERCLASS);
		if (presents(rowSuperclassName)) {
			rowSuperclass = Class.forName(rowSuperclassName);//名称だけなので通常のload
		} else {
			rowSuperclass = null;
		}

		CodeFormatter codeFormatter;
		String codeFormatterClassName = params.options.getProperty(CODE_FORMATTER_CLASS);
		if (presents(codeFormatterClassName)) {
			codeFormatter = (CodeFormatter) Class.forName(codeFormatterClassName).getDeclaredConstructor().newInstance();
		} else {
			codeFormatter = null;
		}

		boolean useNumberClass;
		String useNumberClassString = params.options.getProperty(USE_NUMBER_CLASS);
		if (presents(useNumberClassString)) {
			useNumberClass = Boolean.parseBoolean(useNumberClassString);
		} else {
			useNumberClass = false;
		}

		boolean notUseNullGuard;
		String notUseNullGuardString = params.options.getProperty(NOT_USE_NULL_GUARD);
		if (presents(notUseNullGuardString)) {
			notUseNullGuard = Boolean.parseBoolean(notUseNullGuardString);
		} else {
			notUseNullGuard = false;
		}

		String driverClass = params.options.getProperty(JDBC_DRIVER_CLASS);
		if (presents(driverClass)) {
			init.put(BlendeeConstants.JDBC_DRIVER_CLASS_NAME, driverClass);
		}

		if (!params.verbose) {
			init.put(BlendeeConstants.LOGGER_CLASS, VoidLogger.class);
		} else {
			init.put(BlendeeConstants.LOGGER_CLASS, SystemOutLogger.class);
		}

		Blendee.start(init);

		if (params.verbose) {
			info(params.toString());
			info("start " + LocalDateTime.now());
		}

		schemaPathStream().forEach(path -> {
			if (!Files.exists(path)) {
				try {
					Files.createDirectories(path);
				} catch (IOException e) {
					throw new CommandException(e);
				}

				if (params.verbose) {
					info("create directory " + path);
				}
			}
		});

		CommandTableFacadeGeneratorHandler handler = new CommandTableFacadeGeneratorHandler();

		Blendee.execute(t -> {
			TableFacadeGenerator generator = new TableFacadeGenerator(
				BlendeeManager.get().getMetadata(),
				params.packageName,
				tableFacadeSuperclass,
				rowSuperclass,
				codeFormatter,
				useNumberClass,
				!notUseNullGuard);

			tables().forEach(table -> handler.add(table));

			handler.execute(generator);
		});

		if (params.verbose) {
			info("end " + LocalDateTime.now());
		}
	}

	private class CommandTableFacadeGeneratorHandler extends TableFacadeGeneratorHandler {

		private final Charset charset;

		private Path currentPath;

		private CommandTableFacadeGeneratorHandler() {
			if (presents(params.encoding)) {
				charset = Charset.forName(params.encoding);
			} else {
				charset = StandardCharsets.UTF_8;
			}
		}

		@Override
		protected boolean exists(TablePath path) {
			return Files.exists(toPath(path));
		}

		@Override
		protected void start(TablePath path) {
			currentPath = toPath(path);
			info(path.toString());
		}

		@Override
		protected boolean exists() {
			return Files.exists(currentPath);
		}

		@Override
		protected void infoSkip() {
			if (!params.verbose) return;
			info("  -> skip");
		}

		@Override
		protected String format(String source) {
			return source;
		}

		@Override
		protected String loadSource() {
			try {
				return new String(Files.readAllBytes(currentPath), charset);
			} catch (IOException e) {
				throw new CommandException(e);
			}
		}

		@Override
		protected void writeSource(String source) {
			byte[] contents = source.getBytes(charset);
			try {
				Files.write(currentPath, contents);
			} catch (IOException e) {
				throw new CommandException(e);
			}

			if (!params.verbose) return;
			info("  -> create file " + currentPath + " (" + contents.length + " bytes)");
		}

		@Override
		protected void end() {
			currentPath = null;
		}

		@Override
		protected File getOutputRoot() {
			return params.output.toFile();
		}

		private Path toPath(TablePath path) {
			return packagePath()
				.resolve(path.getSchemaName())
				.resolve(TableFacadeGenerator.createCompilationUnitName(path.getTableName()));
		}
	}

	static void info(String message) {
		BlendeeManager.get().getConfigure().getLogger().log(Level.INFO, message);
	}

	private Path packagePath() {
		Path[] path = { params.output };
		Arrays.stream(params.packageName.split("\\.")).map(p -> Paths.get(p)).forEach(p -> path[0] = path[0].resolve(p));
		return path[0];
	}

	private Stream<Path> schemaPathStream() {
		Path packagePath = packagePath();
		return Arrays.stream(params.schemaNames).map(s -> packagePath.resolve(TableFacadePackageRule.care(s)));
	}

	private Stream<TablePath> tables() {
		if (params.tables.length > 0) {
			return Arrays.stream(params.tables).map(TablePath::parse);
		}

		Set<TablePath> tables = new LinkedHashSet<>();
		Arrays.stream(params.schemaNames).flatMap(s -> loadAllTables(s)).forEach(tables::add);

		if (params.regenerate) {
			return schemaPathStream().flatMap(s -> list(s)).map(t -> convert(t)).filter(tables::contains);
		}

		return tables.stream();
	}

	private static TablePath convert(Path table) {
		return new TablePath(
			table.getParent().getFileName().toString(),
			table.getFileName().toString().replace(".java", ""));
	}

	private static Stream<Path> list(Path directory) {
		try {
			return Files.list(directory);
		} catch (IOException e) {
			throw new CommandException(e);
		}
	}

	private static Stream<TablePath> loadAllTables(String schemaName) {
		return Arrays.stream(
			BlendeeManager.get().getMetadata().getTables(schemaName));
	}

	private static boolean presents(String value) {
		return value != null && !value.equals("");
	}
}
