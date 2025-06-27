# Experiment script for ProffDiff
#
# Usage: ./run-experiment.sh <benchmark> <workload> <experiment_name> [output_dir] [runs] [iterations]
# Example: ./run-experiment.sh renaissance mnemonics baseline /home/user2/experiments 30 100

#!/bin/bash

# Default values
OUTPUT_DIR=""
RUNS=""
ITERATIONS=""
HOME_DIR="/home/user2/variability-measurements-infrastructure"
VERSIONS=("23" "24")

# Print usage function
usage() {
		echo "Usage: ./run-experiment.sh -b <benchmark> -w <workload> -e <experiment_name> [-d <output_dir>] [-r <runs>] [-i <iterations>]"
		echo "Example:"
		echo "	./run-experiment.sh -b renaissance -w mnemonics -e baseline -d /home/user2/experiments -r 30 -i 100"
		exit 1
}

# Parse flags
while getopts ":b:w:e:d:r:i:" opt; do
	case $opt in
		b) BENCHMARK="$OPTARG" ;;
		w) WORKLOAD="$OPTARG" ;;
		e) EXPERIMENT_NAME="$OPTARG" ;;
		d) OUTPUT_DIR="$OPTARG/$EXPERIMENT_NAME" ;;
		r) RUNS="$OPTARG" ;;
		i) ITERATIONS="$OPTARG" ;;
		\?) echo "Invalid option -$OPTARG" >&2; usage ;;
		:) echo "Option -$OPTARG requires an argument." >&2; usage ;;
	esac
done

# Check for required arguments
if [ -z "$BENCHMARK" ] || [ -z "$WORKLOAD" ] || [ -z "$EXPERIMENT_NAME" ]; then
	echo "Error: Missing required arguments." >&2
	usage
fi

# Set default OUTPUT_DIR if not specified
if [ -z "$OUTPUT_DIR" ]; then
		OUTPUT_DIR="./runs-$WORKLOAD-$EXPERIMENT_NAME"
fi

# Set default RUNS if not specified
if [ -z "$RUNS" ]; then
		RUNS=30
fi

# Set default ITERATIONS based on BENCHMARK
if [ -z "$ITERATIONS" ]; then
		if [ "$BENCHMARK" = "renaissance" ]; then
				ITERATIONS=100
		else
				ITERATIONS=50
		fi
fi

echo "Benchmark: $BENCHMARK"
echo "Workload: $WORKLOAD"
echo "Experiment Name: $EXPERIMENT_NAME"
echo "Output Dir: $OUTPUT_DIR"
echo "Runs: $RUNS"
echo "Iterations: $ITERATIONS"

mkdir -p "$OUTPUT_DIR"

# EXPERIMENT

print_environment_metadata() {
	echo MACHINE_TYPE="cluster"
	echo PLATFORM_NAME="graalvm-${VERSION}"
	echo NODE_NAME="node6"
	echo KERNEL_VERSION="5.15.0-25-generic"
	if [ "$VERSION" = "23" ]; then
		echo JAVA_VERSION="java 23.0.1 2024-10-15"
	elif [ "$VERSION" = "24" ]; then
		echo JAVA_VERSION="java 24 2025-03-18"
	else
		echo "Unsupported GraalVM version: $VERSION"
	fi
	echo LIBC_VERSION="2.35"
	echo PATCH_COMMIT=""
	echo PATCH_UPSTREAM=""
}

run_graal_version() {
	local version=$1
	
	CSV_DIR="csv-${version}"
	JAVA_CMD="$HOME_DIR/platforms/graalvm-${version}/files/bin/java"
	
	mkdir -p "${OUTPUT_DIR}/${CSV_DIR}"

	for run in $(seq 1 $RUNS); do
		local formatted_run=$(printf "%03d" $run)
		local PROFTOOL_DIR="proftool_graalvm-${version}_${run}"
		local CSV_FILE="${BENCHMARK}@${WORKLOAD}_${formatted_run}.csv"

		mkdir -p "${OUTPUT_DIR}/${PROFTOOL_DIR}"

		echo "Starting benchmark: $WORKLOAD, run: $run, graal: $version"

		# === grouped java flags ===

		# 1. Core server mode
		SERVER_FLAGS=(
			-server
		)

		# 2. JVMCI enablement
		JVMCI_FLAGS=(
			-XX:+UnlockExperimentalVMOptions
			-XX:+UnlockDiagnosticVMOptions
			-XX:+PrintInlining
			-XX:+EnableJVMCI
			-XX:+UseJVMCICompiler
			-Djvmci.Compiler=graal
			-XX:+UseG1GC
			-Djava.security.manager=allow
		)

		# 3. Compilation logging & debug
		COMP_LOG_FLAGS=(
			-XX:+LogCompilation
			-XX:LogFile="${OUTPUT_DIR}/${PROFTOOL_DIR}/log_compilation"
			-XX:+DebugNonSafepoints
			-Djdk.graal.DumpOnError=true
			-Djdk.graal.ShowDumpFiles=true
			-Djdk.graal.PrintGraph=Network
			-Djdk.graal.TrackNodeSourcePosition=true
			-Djdk.graal.OptimizationLog=Directory
			-Djdk.graal.OptimizationLogPath="${OUTPUT_DIR}/${PROFTOOL_DIR}/optimization_log"
		)

		# 4. GraalVM tuning
		GRAAL_TUNE_FLAGS=(
			-Djdk.graal.CompilationFailureAction=Diagnose
			-Djdk.graal.ObjdumpExecutables=objdump,gobjdump
			-Djdk.graal.CompilerConfiguration=community
			-Djdk.graal.TrackNodeSourcePosition=true
			-Djdk.graal.OptimizationLog=Directory
			-Djdk.graal.OptimizationLogPath="${PROFTOOL_DIR}/optimization_log"
		)

		# 5. Memory settings
		MEMORY_FLAGS=(
			-Xss2M
			-Xms12g
			-Xmx12g
		)

		# Set EXPORT_AGENT_FLAGS and BENCH_FLAGS based on BENCHMARK
		if [ "$BENCHMARK" = "renaissance" ]; then
				EXPORT_AGENT_FLAGS=(
						--add-exports=java.base/jdk.internal.misc=jdk.graal.compiler
						-Dgraalvm.locatorDisabled=true
						-agentpath:"/home/user2/mx/mxbuild/linux-amd64-jdk24/com.oracle.jvmtiasmagent/linux-amd64/glibc/libjvmtiasmagent.so=${OUTPUT_DIR}/${PROFTOOL_DIR}/jvmti_asm_file"
						-agentpath:"$HOME_DIR/benchmarks/renaissance/files/libubench-agent.so"
				)

				BENCH_FLAGS=(
						-jar "$HOME_DIR/benchmarks/renaissance/files/renaissance-gpl-0.15.0.jar"
						--plugin "$HOME_DIR/benchmarks/renaissance/files/plugin-ubenchagent-assembly-0.0.1.jar"
						--with-arg JVM:compilations,PAPI:ref-cycles,PAPI:instructions,PAPI:cache-references,PAPI:cache-misses,PAPI:branch-instructions,PAPI:branch-misses
						--plugin "$HOME_DIR/benchmarks/renaissance/files/plugin-jmxmemory-assembly-0.0.1.jar"
						--plugin "$HOME_DIR/benchmarks/renaissance/files/plugin-jmxtimers-assembly-0.0.2.jar"
						--csv "${OUTPUT_DIR}/${CSV_DIR}/${CSV_FILE}"
						--repetitions "$ITERATIONS" "$WORKLOAD"
				)

		elif [ "$BENCHMARK" = "dacapo" ]; then
				EXPORT_AGENT_FLAGS=(
						--add-exports=java.base/jdk.internal.misc=jdk.graal.compiler
						-Dgraalvm.locatorDisabled=true
						-agentpath:"/home/user2/mx/mxbuild/linux-amd64-jdk24/com.oracle.jvmtiasmagent/linux-amd64/glibc/libjvmtiasmagent.so=${OUTPUT_DIR}/${PROFTOOL_DIR}/jvmti_asm_file"
						-Djava.security.manager=allow
						-Dsys.ai.h2o.debug.allowJavaVersions=24
						-agentpath:"$HOME_DIR/benchmarks/dacapo-chopin/files/libubench-agent.so"
				)

				BENCH_FLAGS=(
						-jar "$HOME_DIR/benchmarks/dacapo-chopin/files/dacapo-23.11-chopin.jar"
						--extra-events JVM_COMPILATIONS@MAIN_THREAD_ISOLATED
						--extra-events ref-cycles,instructions,cache-references,cache-misses,branch-instructions,branch-misses@MAIN_THREAD_INHERITED "$WORKLOAD"
						--output-file "${OUTPUT_DIR}/${CSV_DIR}/${CSV_FILE}"
						--iterations "$ITERATIONS"
						--warmup-limit 300
				)
		else
				echo "Unsupported benchmark: $BENCHMARK"
				exit 1
		fi


		# === invoking java with all groups ===

		echo "Running perf record..."
		echo "Command: perf record -k 1 --freq 1000 --event cycles --output ${OUTPUT_DIR}/${PROFTOOL_DIR}/perf_binary_file ${JAVA_CMD} [args]"

		# Optionally: try without perf first to validate Java args


		perf record -k 1 --freq 1000 --event cycles --output "${OUTPUT_DIR}/${PROFTOOL_DIR}/perf_binary_file" \
		"${JAVA_CMD}" \
		"${SERVER_FLAGS[@]}" \
		"${JVMCI_FLAGS[@]}" \
		"${COMP_LOG_FLAGS[@]}" \
		"${MEMORY_FLAGS[@]}" \
		"${EXPORT_AGENT_FLAGS[@]}" \
		"${BENCH_FLAGS[@]}"

		print_environment_metadata > "${OUTPUT_DIR}/${CSV_DIR}/${BENCHMARK}@${WORKLOAD}_${formatted_run}.meta"

		mx profjson -E "${OUTPUT_DIR}/$PROFTOOL_DIR" -o "${OUTPUT_DIR}/${CSV_DIR}/run-${run}.json"
		mx profdiff report "${OUTPUT_DIR}/$PROFTOOL_DIR/optimization_log" "${OUTPUT_DIR}/${CSV_DIR}/run-${run}.json" >> "${OUTPUT_DIR}/${CSV_DIR}/run-${run}.log"

		echo "Completed benchmark: $WORKLOAD, run: $run, graal: $version"
	done
}

for version in "${VERSIONS[@]}"; do

	run_graal_version "$version"

done

rm -rf harness-*
rm -rf launcher-*

mkdir -p "${OUTPUT_DIR}/jit-reports"

for version in "${VERSIONS[@]}"; do

	CSV_DIR="csv-${version}"

	for first in $(seq 1 $RUNS); do
		for second in $(seq $((first + 1)) $RUNS); do

		PROFTOOL_DIR1="proftool_graalvm-${version}_${first}"
		PROFTOOL_DIR2="proftool_graalvm-${version}_${second}"

		mx profdiff jit-vs-jit \
		"${OUTPUT_DIR}/${PROFTOOL_DIR1}/optimization_log/" \
		"${OUTPUT_DIR}/${CSV_DIR}/run-${first}.json" \
		"${OUTPUT_DIR}/${PROFTOOL_DIR2}/optimization_log/" \
		"${OUTPUT_DIR}/${CSV_DIR}/run-${second}.json" \
		> "${OUTPUT_DIR}/jit-reports/graal-${version}_run-${first}-vs-run-${second}.log"

		done
	done
done

# Initialize empty strings
optimization_list=""
json_list=""

for version in "${VERSIONS[@]}"; do
	CSV_DIR="csv-${version}"
	for i in $(seq 1 $RUNS); do
		optimization_list+="${OUTPUT_DIR}/proftool_graalvm-${version}_${i}/optimization_log,"
		json_list+="${OUTPUT_DIR}/${CSV_DIR}/run-${i}.json,"
	done
done

optimization_list=${optimization_list%,}
json_list=${json_list%,}

mx profdiff jit-bulk "$optimization_list" "$json_list" $EXPERIMENT_NAME > $EXPERIMENT_NAME.log

source "${HOME_DIR}/.venv/bin/activate"
EXPERIMENT_ARGS=()
for version in "${VERSIONS[@]}"; do
		CSV_DIR="csv-${version}"
		EXPERIMENT_ARGS+=(--experiment "$EXPERIMENT_NAME" "${OUTPUT_DIR}/${CSV_DIR}/")
done

# Now run the command with all versioned inputs
"${HOME_DIR}/collect-csv.py" \
		-o "${OUTPUT_DIR}/${WORKLOAD}-${EXPERIMENT_NAME}.csv" \
		"${EXPERIMENT_ARGS[@]}"


