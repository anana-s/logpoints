#!/bin/bash
set -e

mkdir -p target/lib
cp target/*.jar target/dependency/*.jar target/lib/
export LOGPOINTS_HOME="target"
export LOGPOINTS_JVM_OPTS="-Xmx64g -Xss1g -XX:+UseSerialGC"

./logpoints serve \
    --prepend \
    --classpath /home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-native-client-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-client-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-nfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-httpfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-rbf-3.3.1.jar: \
    --tag Logger.trace \
    --tag Logger.debug \
    --tag Logger.info \
    --tag Logger.warn \
    --tag Logger.error \
    --tag println \
    org.apache.hadoop.hdfs.server.namenode.NameNode.main
