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
	private final StringArgument outputDirectoryArgument;

	public JITBulkCommand() {
		argumentParser = new ArgumentParser();

		optimizationLogArgument = argumentParser.addStringArgument("optimization_logs", "Array of directories containing optimization logs of the JIT experiment.");
		proftoolOutputArgument = argumentParser.addStringArgument("proftool_outputs", "Array of proftool output files in JSON format for the JIT experiment.");
		experimentName = argumentParser.addStringArgument("experiment_names", "Optional names for the experiments, used for better identification in the output.");
		outputDirectoryArgument = argumentParser.addStringArgument("output_directory", "Directory where the output files will be written.");
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
			try {
				String optimizationLog = entry.getKey();
				String proftoolOutput = entry.getValue();
				
				Experiment experiment = ExperimentParser.parseOrPanic(ExperimentId.ONE, Experiment.CompilationKind.JIT, proftoolOutput, optimizationLog, writer);
				writer.getOptionValues().getHotCompilationUnitPolicy().markHotCompilationUnits(experiment);
				writer.writeln();

				String name = proftoolOutput.substring(proftoolOutput.lastIndexOf('/') + 1, proftoolOutput.lastIndexOf('.'));

				experiment.writeHotMethodsCSV(writer, experimentName.getValue(), name, optimizationLog, proftoolOutput, outputDirectoryArgument.getValue());
			}
			catch (IOException ex) {
				writer.writeln("Error processing experiment with optimization log '" + entry.getKey() + "' and proftool output '" + entry.getValue() + "': " + ex.getMessage());
			}
		}

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