# THIS PROJECT IS NO LONGER ACTIVE! #

Please direct your inquiries to [Killrvideo-2025](https://github.com/KillrVideo/killrvideo-java-2025)


## DEPRECATED! - DEPRECATED! - DEPRECATED! ##

...

# KillrVideo Java #

[![License Apache2](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)

[![Build Status](https://travis-ci.org/KillrVideo/killrvideo-java.svg?branch=master)](https://travis-ci.org/KillrVideo/killrvideo-java)

A reference application for Java developers looking to learn more about using [Apache Cassandra][cassandra] and
[DataStax Enterprise][dse] in their applications and services. Learn more at [Killrvideo].

## The latest stable build of KillrVideo Java is [v2.1.0][v2.1.0]. Master is experimental. Please use v2.1.0 if you attempting to follow the instructions in the links below.

## Building Locally

**Docker Way**

`docker run -v ${PWD}:/opt/killrvideo-java -w /opt/killrvideo-java maven mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B`

**Maven Way**

`mvn install`

## Running Locally

Use these guides to get started running KillrVideo locally on your development machine:
* [Getting Started with KillrVideo][getting-started]: Follow this to setup common dependencies like Docker.
* [Getting Started with Java][getting-started-java]: Follow this to get this Java code 
running.  

## Pull Requests, Requests for More Examples
This project will continue to evolve along with Cassandra and you can expect that as Cassandra and the DataStax 
driver add new features. This sample application will try and provide examples of those. 

We will gladly accept any pull requests for bug fixes, new features, etc.  and if you have a request for an example 
that you don't see in the code currently, send me a message [@SonicDMG][twitter] or [@clunven][clunTwitter] on Twitter or open an issue 
[here][issues] on GitHub.

## License
Copyright 2018 David Gilardi, Cedrick Lunven, derived from original work by Duy Hai Doan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[cassandra]: http://cassandra.apache.org/
[dse]: http://www.datastax.com/products/datastax-enterprise 
[Killrvideo]: https://killrvideo.github.io
[getting-started]: https://killrvideo.github.io/getting-started/
[getting-started-java]: https://killrvideo.github.io/docs/languages/java/
[twitter]: https://twitter.com/SonicDMG
[clunTwitter]: https://twitter.com/clunven
[DSE Java driver API docs]: https://github.com/datastax/java-dse-driver
[DSE Java driver]: https://docs.datastax.com/en/developer/java-driver-dse/1.6/
[issues]: https://github.com/KillrVideo/killrvideo-java/issues
[gremlindsljava]: https://www.datastax.com/dev/blog/gremlin-dsls-in-java-with-dse-graph
[v2.1.0]: https://github.com/KillrVideo/killrvideo-java/tree/v2.1.0
