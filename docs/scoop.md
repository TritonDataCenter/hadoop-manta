# Apache Sqoop Installation

*Note: These instructions are for Sqoop2 (v1.99+)*

To install in Apache Sqoop, copy the jar file `hadoop-manta-x.x-jar-with-dependencies.jar`
to `$SQOOP_HOME/server/lib/` or put the jar file in a Hadoop library path that
will be read by Sqoop's classloader.

Then to setup Sqoop to read/write from Manta, you will need to do the following:

Query the available connectors. Note the id of the HdfsConnector. In this case, it is 3.
```
sqoop:000> show connector 
+----+------------------------+---------+------------------------------------------------------+----------------------+
| Id |          Name          | Version |                        Class                         | Supported Directions |
+----+------------------------+---------+------------------------------------------------------+----------------------+
| 1  | generic-jdbc-connector | 1.99.6  | org.apache.sqoop.connector.jdbc.GenericJdbcConnector | FROM/TO              |
| 2  | kite-connector         | 1.99.6  | org.apache.sqoop.connector.kite.KiteConnector        | FROM/TO              |
| 3  | hdfs-connector         | 1.99.6  | org.apache.sqoop.connector.hdfs.HdfsConnector        | FROM/TO              |
| 4  | kafka-connector        | 1.99.6  | org.apache.sqoop.connector.kafka.KafkaConnector      | TO                   |
+----+------------------------+---------+------------------------------------------------------+----------------------+
```

Create JDBC link against connector id 1.
```
sqoop:000> create link -c1   
Creating link for connector with id 1
Please fill following values to create new link object
Name: PostgreSQL-Manta

Link configuration

JDBC Driver Class: org.postgresql.Driver
JDBC Connection String: jdbc:postgresql://database.host.name/postgres
Username: postgres
Password: 
JDBC Connection Properties: 
There are currently 0 values in the map:
entry# 
New link was successfully created with validation status OK and persistent id 1
```

Create a new link for Manta (please change values as appropriate):
```
sqoop:000> create link -c 3
Creating link for connector with id 3
Please fill following values to create new link object
Name: Manta

Link configuration

HDFS URI: manta:///username/stor/sqoop
Hadoop conf directory: /opt/hadoop-2.8.0/etc/hadoop
New link was successfully created with validation status OK and persistent id 2
```

Setup JDBC output to go to the correct path on Manta in the job configuration:
```
sqoop:000> create job -f 1 -t 2
Creating job for links with from id 1 and to id 2
Please fill following values to create new job object
Name: PostgreSQL-Manta

From database configuration

Schema name: public
Table name: test
Table SQL statement: 
Table column names: 
Partition column name: 
Null value allowed for the partition column: 
Boundary query: 

Incremental read

Check column: 
Last value: 

To HDFS configuration

Override null value: 
Null value: 
Output format: 
  0 : TEXT_FILE
  1 : SEQUENCE_FILE
Choose: 0
Compression format: 
  0 : NONE
  1 : DEFAULT
  2 : DEFLATE
  3 : GZIP
  4 : BZIP2
  5 : LZO
  6 : LZ4
  7 : SNAPPY
  8 : CUSTOM
Choose: 0
Custom compression format: 
Output directory: manta:///username/stor/sqoop
Append mode: 

Throttling resources

Extractors: 
Loaders: 
New job was successfully created with validation status OK  and persistent id 1
```

Then the job can be run. Be sure that the directory specified in the job above
is empty or else the job will fail.

```
sqoop:000> start job -j 1
Submission details
Job ID: 1
Server URL: http://localhost:12000/sqoop/
Created by: root
Creation date: 2016-06-08 17:02:01 UTC
Lastly updated by: root
External ID: job_local2015389814_0002
        http://localhost:8080/
2016-06-08 17:02:12 UTC: SUCCEEDED
```

This job then dumped the output its output in /username/stor/sqoop:

```
$ mls ~~/stor/sqoop/
223d3e02-5a80-4cef-8a04-302511680a7a.txt
2c8b41ff-16cf-43db-b040-7479db80851f.txt
5b37a6ca-0731-4b49-ac2c-8b1285ce9f8a.txt
5d6fb464-7e58-4dce-83ca-126f6d4f8f78.txt
8b826602-e4c9-4827-b6f9-3e445560ca42.txt
8f4bc986-097a-4918-805c-ba85c97cb92c.txt
d812cd90-e6c2-4d90-8008-0dbb06bc0fde.txt
e8182976-bc6d-46d9-80a6-a0a6589ec7ec.txt
ef8ec74f-181c-456e-9847-de1b9335a98a.txt
fcc10de6-efc8-49f3-9635-b5915540e425.txt

```