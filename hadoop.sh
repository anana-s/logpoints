#!/usr/bin/env -S bash -e

mvn install 1>&2

java -Dorg.slf4j.simpleLogger.defaultLogLevel=trace -XX:+UseSerialGC -Xmx90g -cp target/logpoints-1.0.0-SNAPSHOT-jar-with-dependencies.jar anana5.sense.logpoints.$1 \
    --prepend \
    --classpath /home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-native-client-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-client-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-nfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-httpfs-3.3.1.jar:/home/anana5/r/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-rbf-3.3.1.jar: \
    --tag Logger.trace \
    --tag Logger.debug \
    --tag Logger.info \
    --tag Logger.warn \
    --tag Logger.error \
    --tag println \
    org.apache.hadoop.hdfs.server.namenode.NameNode
