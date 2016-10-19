[![Build Status](https://travis-ci.org/joyent/hadoop-manta.svg?branch=master)](https://travis-ci.org/joyent/hadoop-manta)

# Manta Filesystem for Hadoop

## Introduction

This project provides a [Hadoop FileSystem](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/filesystem/introduction.html)
for the open source [Manta object store](https://github.com/joyent/manta). Unlike
other object stores, Manta is [strongly consistent](http://dtrace.org/blogs/dap/2013/07/03/fault-tolerance-in-manta/)
and uses a hierarchical file system (like a Unix filesystem) to organize file storage
whereas S3/Swift use a key/value system. This model aligns closely with the Hadoop
Filesystem model and it leaves us with very few divergences from the default
behavior.

This project is licensed under the [Apache 2.0 license](LICENSE.txt).

## Quirks / Divergences
 * There is no HDFS functionality natively implemented. All files are accessed
   via network data transfer.
 * Manta URIs take the form of `manta:///user.name/stor/path/to/file`
 * Configuration can be done using Hadoop configuration parameters, Java system
   properties or environment variables.
 * Append is not supported.
 * Truncate is only supported for truncating a file to zero bytes.
 * Checksums are performed using md5.
 * Checksums for portions of large files are done remotely using Manta jobs.
 * Disk space used statistics are based on usage reports which aren't updated
   instantaneously.
 * Globally the default replication factor is 2.
 * No assumptions about blocksize are made.
 * setWriteChecksum() isn't supported yet.

## Run Requirements
 * Java 8
 * Hadoop 2.7.2
 * A running instance of the Manta object store ([available on the public cloud](https://www.joyent.com/object-storage))

## Build Requirements
 * Java 8
 * Maven 3.0+
 
## Configuration

You will need to have the public/private keys needed to access Manta on the machine
in which Hadoop is running. It is often best to verify that these keys are setup
correctly using the [Node.js Manta CLI](https://www.npmjs.com/package/manta).

Configuration will be done using the Hadoop configuration files or environment
variables. Refer to the table below for the available configuration options.

### Configuration Parameters

Configuration parameters take precedence from left to right - values on the
left are overridden by values on the right.

| Default                              | System Prop / Hadoop Prop | Environment Variable      |
|--------------------------------------|---------------------------|---------------------------|
| https://us-east.manta.joyent.com:443 | manta.url                 | MANTA_URL                 |
|                                      | manta.user                | MANTA_USER                |
|                                      | manta.key_id              | MANTA_KEY_ID              |
| $HOME/.ssh/id_rsa                    | manta.key_path            | MANTA_KEY_PATH            |
|                                      | manta.key_content         | MANTA_KEY_CONTENT         |
|                                      | manta.password            | MANTA_PASSWORD            |
| 20000                                | manta.timeout             | MANTA_TIMEOUT             |
| 3                                    | manta.retries             | MANTA_HTTP_RETRIES        |
| 24                                   | manta.max_connections     | MANTA_MAX_CONNS           |
| ApacheHttpTransport                  | manta.http_transport      | MANTA_HTTP_TRANSPORT      |
| TLSv1.2                              | https.protocols           | MANTA_HTTPS_PROTOCOLS     |
| <value too big - see code>           | https.cipherSuites        | MANTA_HTTPS_CIPHERS       |
| false                                | manta.no_auth             | MANTA_NO_AUTH             |
| false                                | manta.disable_native_sigs | MANTA_NO_NATIVE_SIGS      |
| 0                                    | http.signature.cache.ttl  | MANTA_SIGS_CACHE_TTL      |

* `manta.url` ( **MANTA_URL** )
The URL of the manta service endpoint to test against
* `manta.user` ( **MANTA_USER** )
The account name used to access the manta service. If accessing via a [subuser](https://docs.joyent.com/public-cloud/rbac/users),
you will specify the account name as "user/subuser".
* `manta.key_id`: ( **MANTA_KEY_ID**)
The fingerprint for the public key used to access the manta service.
* `manta.key_path` ( **MANTA_KEY_PATH**)
The name of the file that will be loaded for the account used to access the manta service.
* `manta.key_content` ( **MANTA_KEY_CONTENT**)
The content of the private key as a string. This is an alternative to `manta.key_path`. Both
`manta.key_path` and can't be specified at the same time `manta.key_content`.
* `manta.password` ( **MANTA_PASSWORD**)
The password associated with the key specified. This is optional and not normally needed.
* `manta.timeout` ( **MANTA_TIMEOUT**)
The number of milliseconds to wait after a request was made to Manta before failing.
* `manta.retries` ( **MANTA_HTTP_RETRIES**)
The number of times to retry failed HTTP requests.
* `manta.max_connections` ( **MANTA_MAX_CONNS**)
The maximum number of open HTTP connections to the Manta API.
* `manta.http_transport` (**MANTA_HTTP_TRANSPORT**)
The HTTP transport library to use. Either the Apache HTTP Client (ApacheHttpTransport) or the native JDK HTTP library (NetHttpTransport).
* `https.protocols` (**MANTA_HTTPS_PROTOCOLS**)
A comma delimited list of TLS protocols.
* `https.cipherSuites` (**MANTA_HTTPS_CIPHERS**)
A comma delimited list of TLS cipher suites.
* `manta.no_auth` (**MANTA_NO_AUTH**)
When set to true, this disables HTTP Signature authentication entirely. This is
only really useful when you are running the library as part of a Manta job.
* `http.signature.native.rsa` (**MANTA_NO_NATIVE_SIGS**)
When set to true, this disables the use of native code libraries for cryptography.
* `http.signature.cache.ttl` (**MANTA_SIGS_CACHE_TTL**)
Time in milliseconds to cache the HTTP signature authorization header. A setting of
0ms disables the cache entirely.

## Hadoop Installation

*Note: These instructions are for Hadoop 2.7.2.*

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
➜  hadoop-2.7.2 ./bin/hdfs dfs -ls 'manta:///username/'
Found 4 items
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/jobs
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/public
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/reports
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/stor
```

or:

```
➜  hadoop-2.7.2 ./bin/hadoop fs -ls 'manta:///username/'

Found 4 items
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/jobs
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/public
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/reports
drwxrwxrwx   -          1 2013-05-22 10:39 manta:///username/stor
```

## Apache Drill Installation

*Note: These instructions are for Apache Drill 1.6.*

To install in Apache Drill, copy the jar file `hadoop-manta-x.x-jar-with-dependencies.jar`
to `$DRILL_HOME/jars/3rdparty/`.

Verify that all of the needed configuration parameters have been set using environment
variables or native Hadoop configuration (often set in 
`$DRILL_HOME/conf/core-site.xml`).

Next, navigate to the Drill web control panel (typically [http://localhost:8047](http://localhost:8047)
and go to the Storage tab. Go to the new *Storage Plugin* field and type in *manta*
then click *Create*.

In the *Configuration* text box, paste the following:

```javascript
{
  "type": "file",
  "enabled": true,
  "connection": "manta:///",
  "config": null,
  "workspaces": {
    "root": {
      "location": "/",
      "writable": false,
      "defaultInputFormat": null
    },
    "tmp": {
      "location": "/tmp",
      "writable": true,
      "defaultInputFormat": null
    }
  },
  "formats": {
    "psv": {
      "type": "text",
      "extensions": [
        "tbl"
      ],
      "delimiter": "|"
    },
    "csv": {
      "type": "text",
      "extensions": [
        "csv"
      ],
      "delimiter": ","
    },
    "tsv": {
      "type": "text",
      "extensions": [
        "tsv"
      ],
      "delimiter": "\t"
    },
    "parquet": {
      "type": "parquet"
    },
    "json": {
      "type": "json",
      "extensions": [
        "json"
      ]
    }
    "avro": {
      "type": "avro"
    },
    "sequencefile": {
      "type": "sequencefile",
      "extensions": [
        "seq"
      ]
    },
    "csvh": {
      "type": "text",
      "extensions": [
        "csvh"
      ],
      "extractHeader": true,
      "delimiter": ","
    }
  }
}
```

Click *Create*.

When you next start up Drill, you can do queries directly against files in the
Manta object store. For example:

```
0: jdbc:drill:zk=local> SELECT * FROM manta.`/username/stor/drill-data/nation.parquet`;
+--------------+-----------------+--------------+-----------------------+
| N_NATIONKEY  |     N_NAME      | N_REGIONKEY  |       N_COMMENT       |
+--------------+-----------------+--------------+-----------------------+
| 0            | ALGERIA         | 0            |  haggle. carefully f  |
| 1            | ARGENTINA       | 1            | al foxes promise sly  |
| 2            | BRAZIL          | 1            | y alongside of the p  |
| 3            | CANADA          | 1            | eas hang ironic, sil  |
| 4            | EGYPT           | 4            | y above the carefull  |
| 5            | ETHIOPIA        | 0            | ven packages wake qu  |
| 6            | FRANCE          | 3            | refully final reques  |
| 7            | GERMANY         | 3            | l platelets. regular  |
| 8            | INDIA           | 2            | ss excuses cajole sl  |
| 9            | INDONESIA       | 2            |  slyly express asymp  |
| 10           | IRAN            | 4            | efully alongside of   |
| 11           | IRAQ            | 4            | nic deposits boost a  |
| 12           | JAPAN           | 2            | ously. final, expres  |
| 13           | JORDAN          | 4            | ic deposits are blit  |
| 14           | KENYA           | 0            |  pending excuses hag  |
| 15           | MOROCCO         | 0            | rns. blithely bold c  |
| 16           | MOZAMBIQUE      | 0            | s. ironic, unusual a  |
| 17           | PERU            | 1            | platelets. blithely   |
| 18           | CHINA           | 2            | c dependencies. furi  |
| 19           | ROMANIA         | 3            | ular asymptotes are   |
| 20           | SAUDI ARABIA    | 4            | ts. silent requests   |
| 21           | VIETNAM         | 2            | hely enticingly expr  |
| 22           | RUSSIA          | 3            |  requests against th  |
| 23           | UNITED KINGDOM  | 3            | eans boost carefully  |
| 24           | UNITED STATES   | 1            | y final packages. sl  |
+--------------+-----------------+--------------+-----------------------+
25 rows selected (1.079 seconds)
```

## Apache Sqoop Installation

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
Hadoop conf directory: /opt/hadoop-2.7.2/etc/hadoop
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
