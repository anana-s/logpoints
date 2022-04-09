docker run -p 7000:7000 -v hadoop-3.3.1-src:/hadoop logpoints \
    --prepend \
    --classpath /hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-3.3.1.jar:/hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-native-client-3.3.1.jar:/hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-client-3.3.1.jar:/hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-nfs-3.3.1.jar:/hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-httpfs-3.3.1.jar:/hadoop/hadoop-3.3.1/share/hadoop/hdfs/hadoop-hdfs-rbf-3.3.1.jar: \
    --tag Logger.trace \
    --tag Logger.debug \
    --tag Logger.info \
    --tag Logger.warn \
    --tag Logger.error \
    --tag println \
    org.apache.hadoop.hdfs.server.namenode.NameNode