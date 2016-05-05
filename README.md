## Notice regarding the Linux Foundation's Hyperledger project

HLP-Candidate is Digital Asset's proposed contribution to the Linux Foundation's [Hyperledger](https://www.hyperledger.org/) project. We have made it available as open source to enable others to explore our architecture and design. Digital Asset's intention is to engage rigorously in the Linux Foundation's [Hyperledger](https://www.hyperledger.org/) project as the community establishes itself, and decides on a code base. Once established, we will transition our development focus to the [Hyperledger](https://www.hyperledger.org/) effort, and this code will be maintained as needed for Digital Asset's use.

We decided to split the HLP-Candidate project into a client and server part. This repository holds the client software.

While we invite contribution to the HLP-Candidate project, we believe that the broader blockchain community's focus should be the [Hyperledger](https://www.hyperledger.org/) project.

This codebase has been renamed to "HLP-Cadidate" but parts of the code reference "hyperledger". This codebase is one of 4 proposal codebases to the official Hyperledger project, and links to the others can be found in the official [Linux Foundation repository](https://github.com/hyperledger/hyperledger). Hyperledger is a trademark of The Linux Foundation. Linux Foundation is a registered trademark of The Linux Foundation. Linux is a registered trademark of Linus Torvalds.

# HLP-Candidate-API
HLP-Candidate comes with a client [API](docs/api.md).

What we are making available today is the most recent stable version of a combination of many man years of work across multiple startups: Digital Asset, Bits of Proof, Blockstack, and Hyperledger. However, it is still a work in progress and we are in the process of replacing several components, adding others, and integrating with other open source projects. This particularly relates to security, scalability, and privacy, and is outlined in the roadmap below.

HLP-Candidate was built with the requirements of enterprise architecture in mind by a team that has worked in financial institutions for decades. It has a highly modular design at both the code and runtime levels to allow for integrations with legacy systems. The networking rules are configurable to allow for distinct interoperable consensus groups, each with its own functional and nonfunctional requirements.

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

## Documentation
 * [API](docs/api.md) (low level API)
 * [Account](docs/accountmodule.md) (high level API)

## Contributing
[How to contribute?](docs/contributing.md)
[Digital Asset's HLP-Candidate Mailing List](https://groups.google.com/a/digitalasset.com/forum/?hl=en#!forum/HLP-Candidate)
