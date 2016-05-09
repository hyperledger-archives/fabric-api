# Incubation Notice
This project is a Hyperledger project in _Incubation_. It was proposed to the community and documented [here](https://goo.gl/RYQZ5N). Information on what _Incubation_ entails can be found in the [Hyperledger Project Lifecycle document](https://goo.gl/4edNRc).

# Fabric-API
Fabric-API comes with a client [API](docs/api.md).

What we are making available today is the most recent stable version of a combination of many man years of work across multiple startups: Digital Asset, Bits of Proof, Blockstack, and Hyperledger. However, it is still a work in progress and we are in the process of replacing several components, adding others, and integrating with other open source projects. This particularly relates to security, scalability, and privacy.

Fabric-API was built with the requirements of enterprise architecture in mind by a team that has worked in financial institutions for decades. It has a highly modular design at both the code and runtime levels to allow for integrations with legacy systems.

## Building and running

### Prerequisites
Version numbers below indicate the versions used.

 * Git 2.4.6 (http://git-scm.com)
 * Maven 3.3.3 (http://maven.apache.org)
 * Java 1.8.0_51 (http://java.oracle.com)
 * JCE 8 (Java Crptography Extension) (http://java.oracle.com)
 * Protobuf compiler 3.0.0-beta2 (http://github.com/google/protobuf)

#### Optionally a JMS bus provider
 * e.g. Apache ActiveMQ 5.11.1 (http://activemq.apache.org/)

#### Installing Prerequisites on OSX
 * ```brew update```
 * ```brew tap homebrew/versions```
 * ```brew install git```
 * ```brew install maven```
 * Download and install the latest Java 8 dmg file from Oracle
 * Download _Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files for JDK/JRE 8_ from Oracle, which is a zip file. Extract it and copy the `local_policy.jar` and `US_export_policy.jar` files to your your `<java_runtime_home>/lib/security`
 * ```brew install protobuf250```
 * ```brew install procmail``` if the command ```lockfile``` is not available on your OSX version

#### Installing Prerequisites on Ubuntu Linux
 * ```add-apt-repository ppa:webupd8team/java```
 * ```apt-get update```
 * ```apt-get install git maven oracle-java8-installer oracle-java8-unlimited-jce-policy protobuf-compiler procmail```

### Building Steps

 * ```git clone ???```
 * ```cd hyperledger```
 * ```mvn clean package```

#### Building secp256 library
 * clone https://github.com/bitcoin-core/secp256k1
 * ./autgen.sh
 * ./configure --enable-module-schnorr --enable-experimental --enable-module-ecdh
 * make
 * copy resulting binary (.libs/libsecp256k1.dylib) to fabric-api/native/src/main/resources/x86_64-MacOSX-gpp/lib/ for OSX
 * copy resulting binary (.libs/libsecp256k1.dylib) to fabric-api/native/src/main/resources/amd64-Linux-gpp/lib/libsecp256k1.so for Linux


## Documentation
 * [API](docs/api.md) (low level API)
 * [Account](docs/accountmodule.md) (high level API)

## Contributing
[How to contribute?](docs/contributing.md)
