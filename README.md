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
 * Hadoop 2.8.0
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

Configuration used are the same as [Java Manta Client SDK](https://github.com/joyent/java-manta#configuration).
Hadoop-specific property keys use the same keys as system properties within
the Java Manta SDK.

## Installation

Please refer to the following guides for installing the Manta Filesystem for
Hadoop on different Hadoop ecosystem applications.

* [Apache Hadoop](docs/hadoop.md)
* [Apache Drill](docs/drill.md)
* [Apache Scoop](docs/scoop.md)

## License

The Manta Filesystem for Hadoop is licensed under the 
[Apache 2.0 license](LICENSE.txt). 
