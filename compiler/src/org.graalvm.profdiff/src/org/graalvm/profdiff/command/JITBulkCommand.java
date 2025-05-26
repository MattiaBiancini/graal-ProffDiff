import java.util.HashMap;

public class JITBulkCommand implements Command {
	private final ArgumentParser argumentParser;

	private final Map<StringArgument, StringArgument> argumentMap;

	public JITBulkCommand() {
		argumentParser = new ArgumentParser();

		argumentMap = new HashMap<>();

		optimizationLogArgument = argumentParser.addStringArgument(
						"optimization_log", "directory with optimization logs of the JIT experiment");
		proftoolArgument = argumentParser.addStringArgument(
						"proftool_output", "proftool output of the JIT experiment in JSON");
	}

	@Override
	public String getName() {
		return "jit-bulk";
	}

	@Override
	public String getDescription() {
		return "Compare JIT-compiled experiments with proftool data in bulk mode.";
	}

	@Override
	public ArgumentParser getArgumentParser() {
		return argumentParser;
	}

	@Override
	public void invoke(Writer writer) throws ExperimentParserError {
		ExplanationWriter explanationWriter = new ExplanationWriter(writer, false, true);
		explanationWriter.explain();

		writer.writeln();

		Experiment experiment = ExperimentParser.parseExperiment(
						ExperimentId.fromString("bulk"), proftoolArgument.getValue(), optimizationLogArgument.getValue());
		ExperimentPair pair = new ExperimentPair(experiment, null);
		pair.write(writer);
	}

}
