package org.graalvm.profdiff.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.profdiff.args.ArgumentParser;
import org.graalvm.profdiff.args.StringArgument;
import org.graalvm.profdiff.core.Experiment;
import org.graalvm.profdiff.core.ExperimentId;
import org.graalvm.profdiff.core.Writer;
import org.graalvm.profdiff.parser.ExperimentParser;
import org.graalvm.profdiff.parser.ExperimentParserError;


/**
 * Compares multiple JIT-compiled experiments with proftool data in bulk mode.
 * This command allows users to specify multiple optimization logs and proftool outputs
 */
public class JITBulkCommand implements Command {
	private final ArgumentParser argumentParser;

	private final Map<String, String> argumentMap = new HashMap<>();
	private final StringArgument optimizationLogArgument;
	private final StringArgument proftoolOutputArgument;
	private final StringArgument experimentName;

	public JITBulkCommand() {
		argumentParser = new ArgumentParser();

		optimizationLogArgument = argumentParser.addStringArgument("optimization_logs", "Array of directories containing optimization logs of the JIT experiment.");
		proftoolOutputArgument = argumentParser.addStringArgument("proftool_outputs", "Array of proftool output files in JSON format for the JIT experiment.");
		experimentName = argumentParser.addStringArgument("experiment_names", "Optional names for the experiments, used for better identification in the output.");

	}

	@Override
	public String getName() {
		return "jit-bulk";
	}

	@Override
	public String getDescription() {

		String description = "Compare JIT-compiled experiments with proftool data in bulk mode.";

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

		loadArguments(optimizationLogArgument, proftoolOutputArgument);

		writer.writeln();

		for (Map.Entry<String, String> entry : argumentMap.entrySet()) {
			String optimizationLog = entry.getKey();
			String proftoolOutput = entry.getValue();

			Experiment jit = ExperimentParser.parseOrPanic(ExperimentId.ONE, Experiment.CompilationKind.JIT, proftoolOutput, optimizationLog, writer);
			writer.getOptionValues().getHotCompilationUnitPolicy().markHotCompilationUnits(jit);
			if (experimentName.getValue() == null || experimentName.getValue().isEmpty()) {
				try {
					jit.writeHotMethodsCSV(writer, jit.getExperimentId().toString());
				} catch (IOException ex) {
					writer.writeln("Error writing hot methods CSV: " + ex.getMessage());
				}
			} else {
				try {
					jit.writeHotMethodsCSV(writer, experimentName.getValue());
				} catch (IOException ex) {
					writer.writeln("Error writing hot methods CSV: " + ex.getMessage());
				}
			}
			writer.writeln();
		}

		// ExperimentMatcher matcher = new ExperimentMatcher(writer);
	}

	private void loadArguments(StringArgument optimizationLogsArgument, StringArgument proftoolOutputsArgument) {
		
		String[] optimizationLogs = optimizationLogsArgument.getValue().split(",");
		String[] proftoolOutputs = proftoolOutputsArgument.getValue().split(",");

		if (optimizationLogs.length != proftoolOutputs.length) {
			throw new IllegalArgumentException("The number of optimization logs must match the number of proftool outputs.");
		}

		for (int i = 0; i < optimizationLogs.length; i++) {
			argumentMap.put(optimizationLogs[i], proftoolOutputs[i]);
		}

	}

}
