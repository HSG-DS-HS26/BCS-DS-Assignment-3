# Spark cluster lab

A Spark 4.1.3 standalone cluster in Docker Compose: 1 master, 3 workers (1 core, 512 MB executor each), a `dev` container that acts as the Spark driver for jobs written in Java, and HDFS. 

The tasks are in [TASKS.md](TASKS.md). This file is the reference for the setup.

## Learning objectives

1. Implement and execute a Spark RDD pipeline for a specified data-processing task and verify the output.
2. Explain the distributed execution of a Spark pipeline using its code and execution history, identifying transformations and actions, partition-level parallelism, and where and why shuffling occurs.
3. Adapt an existing Spark pipeline to a changed processing requirement, justify the chosen operations, and verify that the revised output satisfies the requirement.
4. Run a Spark pipeline on input stored in HDFS and explain how HDFS stores and replicates the data and how Spark and HDFS handle a worker failure.

## Setup

In VS Code or Codespaces, choose "Reopen in Container". All containers start automatically and you get a terminal inside `dev` (root@dev).

Without VS Code, run `docker compose up -d --build`, then `docker compose exec dev bash`.

## UIs

| Port | What |
|------|------|
| 18080 | History Server: jobs, stages, tasks and DAGs of every run. A run shows up a few seconds after it ends; running jobs are under "Show incomplete applications". |
| 8080 | Spark master: registered workers and running applications |
| 9870 | HDFS NameNode: DataNodes, capacity, file browser |

Locally, open them at `http://localhost:<port>`. In Codespaces, open them from the Ports tab.

If a port is missing from the Ports tab, run `bash .devcontainer/ports.sh` in the terminal, then click **Forward a Port** in the Ports tab and enter the port number.

## AI Usage Disclaimer

LLMs were used in the preparation of this assignment template to support the development process, for example, for code scaffolding and layout. The learning objectives, structure, and content of the assignment, including all student-facing templates and materials, were designed by the TAs and the coordinating professor. Any LLM-generated code was reviewed and tested before release.
