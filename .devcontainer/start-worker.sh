#!/bin/bash
# Runs an HDFS DataNode next to the Spark worker, so blocks live on the same host
# as the executors that read them (data locality). Both register under the container
# hostname; otherwise Spark sees IPs, HDFS sees names, and no read counts as local.
export SPARK_LOCAL_HOSTNAME="$(hostname)"

# If the DataNode stops (./run.sh ... --problematic-system shuts it down), it starts
# again after 2 minutes. That is long enough for the NameNode to declare it dead
# (~70 s) and to start copying its blocks to the other DataNodes.
(
    while true; do
        hdfs datanode -D dfs.datanode.hostname="$SPARK_LOCAL_HOSTNAME"
        sleep 120
    done
) &
exec /opt/spark/bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
