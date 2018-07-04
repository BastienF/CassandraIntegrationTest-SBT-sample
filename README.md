CassandraIntegration-SBT-sample
=========
Sample of SBT project to bootstrap a scala projet with dockerised Cassandra tests

The purpose of this repository is to allow you to include in your SBT/Scala project some code to handle Cassandra
integration tests based on a Datastax Entreprise docker container that will pop up on each it:test execution.


Requirements
------------
The following packages have to be installed and well configured :
- [Docker-ce](https://docs.docker.com/engine/installation/)
- GNU getopt: [For MacOS Users](https://stackoverflow.com/questions/12152077/how-can-i-make-bash-deal-with-long-param-using-getopt-command-in-mac)
- [SBT v1.1+](https://www.scala-sbt.org/)
- [Scala v2.12+](https://www.scala-lang.org/)

Usage of this sample for Demo purpose
-------------
This projet is a simple interaction with a Cassandra cluster.

It create a dummy key-value table `sample` inside a keyspace `sample_keyspace` and insert/retrieve some data in it.

You can find a sample of unit testing in the following location : `src/test/scala/ovh/moi/cassandra/SampleRepository_Test.scala`.

To execute those unit tests: `sbt test`

You can find a sample of integration testing in the following location : `src/testIt/scala/ovh/moi/cassandra/SampleRepository_ITTest.scala`.

To execute those unit tests: `sbt it:test`.
This command will run a new Cassandra docker container and play the tests against this cluster.


Usage of this sample inside your SBT/Scala project
-------------
//TODO


License
-------

MIT
