#!/bin/bash
# Compile src/ against the jars of the Spark install (so the versions always match
# the cluster), package it as build/jobs.jar and submit the job of a task folder
# src/<name>/ (any folder except src/common/).
#
#   ./run.sh 01_wordcount                       # src/01_wordcount, data from /data
#   ./run.sh 01_wordcount hdfs                  # data from HDFS
#   ./run.sh 03_pagerank 3                      # other words go to the job (3 iterations)
#   ./run.sh 02_join hdfs --problematic-system  # a worker crashes during the job
#
# After the task folder, the words can come in any order:
#   <job>                   part of a class name, only needed if the folder has several jobs
#   local | hdfs            where the data is read from (default: local, i.e. /data)
#   --problematic-system    kill the executor and the DataNode of a worker after the
#                           first shuffle stage (see src/common/ProblematicSystem.java)
#   anything else           passed on to the job, after the base path
set -euo pipefail
cd "$(dirname "$0")"

usage() {
    echo "usage: ./run.sh <task folder> [job] [local|hdfs] [--problematic-system] [job args...]" >&2
    echo >&2
    for dir in src/*/; do
        [ "$dir" = src/common/ ] && continue
        task=$(basename "$dir")
        mains=$(grep -l 'static void main' "$dir"*.java | sort)
        for f in $mains; do
            class=$(basename "$f" .java)
            if [ "$(echo "$mains" | wc -l)" -eq 1 ]; then job=; else job=$(echo "$class" | tr '[:upper:]' '[:lower:]'); fi
            printf '  ./run.sh %-32s %s\n' "$task $job" "$dir$class.java" >&2
        done
    done
    exit 1
}

[ $# -ge 1 ] || usage
# "01_wordcount", "src/01_wordcount" and "src/01_wordcount/" (tab completion) all work.
name=${1#src/}
name=${name%/}
dir=src/$name/
[[ "$name" =~ ^[a-z0-9_]+$ ]] && [ "$name" != common ] && [ -d "$dir" ] || { echo "no task folder src/$name" >&2; usage; }
shift

mains=$(grep -l 'static void main' "$dir"*.java | sort)
job=
storage=
conf=()
jobargs=()
for arg in "$@"; do
    word=$(echo "$arg" | tr '[:upper:]' '[:lower:]')
    if [ "$word" = --problematic-system ]; then
        conf+=(--conf spark.extraListeners=ProblematicSystem)
        continue
    elif [ -z "$storage" ] && { [ "$word" = local ] || [ "$word" = hdfs ]; }; then
        storage=$word
        continue
    elif [ -z "$job" ] && [ -n "$word" ]; then
        matches=$(for f in $mains; do basename "$f" .java; done | grep -i -F -- "$word" || true)
        if [ "$(echo "$matches" | grep -c .)" -eq 1 ]; then
            job=$matches
            continue
        elif [ -n "$matches" ]; then
            echo "'$arg' matches several jobs: $(echo $matches)" >&2
            exit 1
        fi
    fi
    jobargs+=("$arg")
done
if [ -z "$job" ]; then
    if [ "$(echo "$mains" | wc -l)" -ne 1 ]; then
        echo "$dir has several jobs, name one:" >&2
        for f in $mains; do echo "  $(basename "$f" .java)" >&2; done
        exit 1
    fi
    job=$(basename "$mains" .java)
fi

if [ "$storage" = hdfs ]; then base=hdfs://namenode:8020/data; else base=/data; fi

# The folders start with a digit, which a Java package name can't, so all classes are in
# the default package and every class name must be unique across src/.
rm -rf build/classes
mkdir -p build/classes
javac -proc:none -cp "/opt/spark/jars/*" -d build/classes $(find src -name '*.java')
jar cf build/jobs.jar -C build/classes .

exec spark-submit ${conf[@]+"${conf[@]}"} --class "$job" build/jobs.jar "$base" ${jobargs[@]+"${jobargs[@]}"}
