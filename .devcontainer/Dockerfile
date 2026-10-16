# Stage "cluster": Spark + HDFS. Used by master, workers, NameNode and History Server.
FROM apache/spark:4.1.3 AS cluster

ARG MAVEN=https://repo1.maven.org/maven2

# DataFlint OSS: adds a "DataFlint" tab to the History Server (18080) and the driver's job UI.
# Installed as a jar because the History Server can't load --packages.
ADD --chmod=644 ${MAVEN}/io/dataflint/dataflint-spark4_2.13/0.9.9/dataflint-spark4_2.13-0.9.9.jar /opt/spark/jars/

# Hadoop 3.4.2 (same version as Spark's Hadoop client) for the HDFS NameNode and DataNodes.
# "lean" leaves out the AWS SDK; YARN, MapReduce and docs are removed as well (only HDFS is used).
USER root
ARG HADOOP_VERSION=3.4.2
RUN set -eux; \
    f="hadoop-${HADOOP_VERSION}-lean.tar.gz"; \
    curl -fsSL -o "/tmp/$f" "https://dlcdn.apache.org/hadoop/common/hadoop-${HADOOP_VERSION}/$f" \
      || curl -fsSL -o "/tmp/$f" "https://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION}/$f"; \
    tar -xzf "/tmp/$f" -C /opt; \
    rm "/tmp/$f"; \
    mv "/opt/hadoop-${HADOOP_VERSION}" /opt/hadoop; \
    rm -rf /opt/hadoop/share/doc /opt/hadoop/share/hadoop/yarn /opt/hadoop/share/hadoop/mapreduce \
           /opt/hadoop/share/hadoop/tools /opt/hadoop/share/hadoop/client; \
    mkdir /opt/hadoop/share/hadoop/yarn /opt/hadoop/share/hadoop/mapreduce  # the launcher checks they exist
ENV HADOOP_HOME=/opt/hadoop \
    PATH="${PATH}:/opt/hadoop/bin"
COPY hadoop/ /opt/hadoop/etc/hadoop/
COPY --chmod=755 start-worker.sh /opt/start-worker.sh
USER spark

# Stage "dev": the driver container you work in.
FROM cluster AS dev

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends git procps curl socat \
    && rm -rf /var/lib/apt/lists/*

# The base image ships a full JDK (javac, jar). run.sh compiles the jobs against
# /opt/spark/jars, so they can never drift from the cluster's Spark version.
ENV PATH="${PATH}:/opt/spark/bin"

COPY spark-defaults.conf /opt/spark/conf/spark-defaults.conf
