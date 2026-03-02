#!/bin/bash

# Die on errors
set -e

# Check for correct number of arguments
if [ "$#" -lt 4 ]; then
    echo "Usage: $0 <revision1> <revision2> <year> <input_file1> [input_file2 ...]"
    exit 1
fi

# Get revisions and year from arguments
REV1=$1
REV2=$2
YEAR=$3
shift 3
INPUT_FILES=("$@")

# Temporary directory for checkouts and builds
WORKDIR="comparison_workdir"
REV1_DIR="$WORKDIR/rev1"
REV2_DIR="$WORKDIR/rev2"
REV1_OUT_DIR="$WORKDIR/out1"
REV2_OUT_DIR="$WORKDIR/out2"

# Clean up previous runs
if [ -d "$WORKDIR" ]; then
    echo "Cleaning up old workdir..."
    rm -rf "$WORKDIR"
fi

# Create directories
mkdir -p "$REV1_DIR" "$REV2_DIR" "$REV1_OUT_DIR" "$REV2_OUT_DIR"

echo "Checking out revisions..."
# Checkout revisions into temporary directories
git worktree add --detach "$REV1_DIR" "$REV1"
git worktree add --detach "$REV2_DIR" "$REV2"

# Function to build and run the application
run_revision() {
    local rev_dir=$1
    local out_dir=$2
    local rev_name=$3

    echo "================================================="
    echo "Building revision $rev_name in $rev_dir"
    echo "================================================="
    (
        cd "$rev_dir"
        mvn package -Dmaven.test.skip=true -q
    )

    # Build classpath
    (cd "$rev_dir" && mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -q)
    local classpath="$rev_dir/target/classes:$(cat "$rev_dir/cp.txt")"

    # Detect main class from jar manifest
    local jar_file
    jar_file=$(ls "$rev_dir"/target/cockroach-*.jar 2>/dev/null | head -1)
    local main_class
    main_class=$(unzip -p "$jar_file" META-INF/MANIFEST.MF | grep Main-Class | awk '{print $2}' | tr -d '\r')
    echo "Main class: $main_class"

    echo "-------------------------------------------------"
    echo "Running tests for revision $rev_name"
    echo "-------------------------------------------------"
    for input_file in "${INPUT_FILES[@]}"; do
        base_name=$(basename "$input_file")
        local report_dir="$out_dir/${base_name}_reports"
        mkdir -p "$report_dir"
        output_file="$out_dir/$base_name.out"
        echo "Running with input: $input_file, output to: $output_file"
        java -cp "$classpath" "$main_class" "$input_file" "$YEAR" "$(pwd)/$report_dir" > "$(pwd)/$output_file" 2>&1
    done
}

# Run for both revisions
run_revision "$REV1_DIR" "$REV1_OUT_DIR" "$REV1"
run_revision "$REV2_DIR" "$REV2_OUT_DIR" "$REV2"

# Compare results
echo "================================================="
echo "Comparison Summary"
echo "================================================="
DIFFERENCES_FOUND=0
for input_file in "${INPUT_FILES[@]}"; do
    base_name=$(basename "$input_file")
    out1="$REV1_OUT_DIR/$base_name.out"
    out2="$REV2_OUT_DIR/$base_name.out"

    echo -n "Comparing stdout for $base_name... "
    if diff -q "$out1" "$out2" >/dev/null; then
        echo "OK"
    else
        echo "DIFFERENT"
        DIFFERENCES_FOUND=1
        echo "----------------- Diff for $base_name stdout -----------------"
        diff -u "$out1" "$out2"
        echo "----------------------------------------------------"
    fi

    # Compare generated report files
    local report_dir1="$REV1_OUT_DIR/${base_name}_reports"
    local report_dir2="$REV2_OUT_DIR/${base_name}_reports"
    if [ -d "$report_dir1" ] && [ -d "$report_dir2" ]; then
        for report_file in "$report_dir1"/*; do
            local report_name
            report_name=$(basename "$report_file")
            local report_file2="$report_dir2/$report_name"
            if [ -f "$report_file2" ]; then
                echo -n "Comparing report $report_name... "
                if diff -q "$report_file" "$report_file2" >/dev/null; then
                    echo "OK"
                else
                    echo "DIFFERENT"
                    DIFFERENCES_FOUND=1
                    echo "----------------- Diff for $report_name -----------------"
                    diff -u "$report_file" "$report_file2"
                    echo "----------------------------------------------------"
                fi
            else
                echo "Report $report_name only in rev1"
                DIFFERENCES_FOUND=1
            fi
        done
        # Check for files only in rev2
        for report_file in "$report_dir2"/*; do
            local report_name
            report_name=$(basename "$report_file")
            if [ ! -f "$report_dir1/$report_name" ]; then
                echo "Report $report_name only in rev2"
                DIFFERENCES_FOUND=1
            fi
        done
    fi
done

# Cleanup
echo "Cleaning up worktrees..."
git worktree remove "$REV1_DIR"
git worktree remove "$REV2_DIR"

if [ "$DIFFERENCES_FOUND" -eq 0 ]; then
    echo "All outputs are identical."
    exit 0
else
    echo "Differences were found."
    exit 1
fi