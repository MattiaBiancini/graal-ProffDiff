import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JITBulkCommand implements Command {
	private final ArgumentParser argumentParser;

	private final Map<StringArgument, StringArgument> argumentMap;

	public JITBulkCommand() {
		argumentParser = new ArgumentParser();

		optimizationLogsArgument = new StringArgument("optimization_logs", "Array of directories containing optimization logs of the JIT experiment.");
		proftoolOutputsArgument = new StringArgument("proftool_outputs", "Array of proftool output files in JSON format for the JIT experiment.");

		loadArguments(optimizationLogsArgument, proftoolOutputsArgument);


	}

	@Override
	public String getName() {
		return "jit-bulk";
	}

	@Override
	public String getDescription() {

		String description = "Compare JIT-compiled experiments with proftool data in bulk mode.";
		description += "\nCorrect usage: " + getName() + " <optimization_logs> <proftool_outputs>";
		description += "\n\t<optimization_logs> - Array of directories containing optimization logs of the JIT experiment.";
		description += "\n\t<proftool_outputs> - Array of proftool output files in JSON format for the JIT experiment.";

		return description;
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

		for (Map.Entry<StringArgument, StringArgument> entry : argumentMap.entrySet()) {
			String optimizationLog = entry.getKey().getValue();
			String proftoolOutput = entry.getValue().getValue();

			Experiment jit = ExperimentParser.parseOrPanic(ExperimentId.ONE, Experiment.CompilationKind.JIT, proftoolOutput.getValue(), optimizationLog.getValue(), writer);
			writer.getOptionValues().getHotCompilationUnitPolicy().markHotCompilationUnits(jit);
			writer.writeln();
		}

		ExperimentMatcher matcher = new ExperimentMatcher(writer);
	}

	private void loadArguments(StringArgument optimizationLogsArgument, StringArgument proftoolOutputsArgument) {
		argumentMap = new HashMap<>();
		
		String[] optimizationLogs = optimizationLogsArgument.getValue().split(",");
		String[] proftoolOutputs = proftoolOutputsArgument.getValue().split(",");

		if (optimizationLogs.length != proftoolOutputs.length) {
			throw new IllegalArgumentException("The number of optimization logs must match the number of proftool outputs.");
		}

		for (int i = 0; i < optimizationLogs.length; i++) {
			argumentMap.put(optimizationLog, proftoolOutput);
		}

	}

}
