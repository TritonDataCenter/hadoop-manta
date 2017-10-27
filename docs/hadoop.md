# Hadoop Installation

*Note: These instructions are for Hadoop 2.8.0.*

To install in Hadoop, copy the jar file `hadoop-manta-x.x-jar-with-dependencies.jar`
to `$HADOOP_HOME/share/hadoop/common/lib/`.

Verify that all of the needed configuration parameters have been set using environment
variables or native Hadoop configuration (often set in 
`$HADOOP_HOME/etc/hadoop/core-site.xml`).

You will need a setting in the core-site.xml file like:

```xml
  <property>
    <name>fs.manta.impl</name>
    <value>com.joyent.hadoop.fs.manta.MantaFileSystem</value>
  </property>
```

Then start Hadoop or any of the Hadoop CLI utilities. For example:

```
➜  hadoop-2.8.0 ./bin/hdfs dfs -ls 'manta:///username/'
Found 4 items
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/jobs
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/public
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/reports
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/stor
```

or:

```
➜  hadoop-2.8.0 ./bin/hadoop fs -ls 'manta:///username/'

Found 4 items
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/jobs
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/public
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/reports
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/stor
```