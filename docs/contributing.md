# Contributing to Hyperledger

Contribute by forking this repo, work in the fork and then and submit pull requests against this repo.

The recommended way to setup the fork is
* Create the fork on github
* In your local working directory `git clone git@github.com:<fork_owner>/hyperledger.git`
* `cd hyperledger`
* `git remote add upstream git@github.com:<linux_foundation_owner>/hyperledger.git`

# API
We build Hyperledger to become the foundation of applications served by an [API](api.md). Changes to the API should be preceeded with in-depth discussion and introduced in accordance with the versioning scheme.

# Versioning
Hyperledger uses major.minor.patch version scheme. Versions within same major and minor but higher patch number should be drop-in backward compatible on API level.
A version with higher minor within the same major will add and might remove features, break API, database or network drop-in backward compatibility. 

# Pull Requests
Pulls should be focused, fix a bug, add a feature or refactore code, but not a mixture.  Bug fixes and features should be accompanied with test. Refactoring with wide scope, such as renaming a frequently used method should be discussed as a feature request and will be applied during dedicated re-factoring windows, that maintainer of the code base will announce regularly.

# Merge approver
You reach merge approver via hyperledger@digitalasset.com

# Bug reports, feature requests
Please use GitHub issue tracker to report bugs and request features.
Alternatively write to hyperledger@digitalasset.com about security relevant bugs.

# Developer notes
We use IntelliJ's default formatting for Java code and Scalariform for scala. Please configure your choice of editor to produce similar layout.
Avoid re-formatting code, limit differences to aid code review.
