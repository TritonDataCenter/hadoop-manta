# Apache Drill Installation

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

```json
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
    },
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