#!/bin/bash

# Die on errors
set -e

# Check for correct number of arguments
if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <revision1> <revision2> <input_file1> [input_file2 ...]"
    exit 1
fi

# Get revisions from arguments
REV1=$1
REV2=$2
shift 2
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
        mvn package -DskipTests
    )

    echo "-------------------------------------------------"
    echo "Running tests for revision $rev_name"
    echo "-------------------------------------------------"
    for input_file in "${INPUT_FILES[@]}"; do
        base_name=$(basename "$input_file")
        output_file="$out_dir/$base_name.out"
        echo "Running with input: $input_file, output to: $output_file"
        java -jar "$rev_dir/target/cockroach-0.1-SNAPSHOT.jar" "$input_file" > "$output_file"
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

    echo -n "Comparing outputs for $base_name... "
    if diff -q "$out1" "$out2" >/dev/null; then
        echo "OK"
    else
        echo "DIFFERENT"
        DIFFERENCES_FOUND=1
        echo "----------------- Diff for $base_name -----------------"
        diff -u "$out1" "$out2"
        echo "----------------------------------------------------"
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