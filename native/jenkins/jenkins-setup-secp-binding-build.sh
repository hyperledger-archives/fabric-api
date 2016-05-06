#!/bin/bash

# debug output for sanity

VERSION=$(grep "<version>" pom.xml | head -1 | cut -d ">" -f 2 | cut -d "<" -f 1)

/bin/echo "VERSION from pom.xml $VERSION"

# below is the script from jenkins
if [[ $VERSION =~ -SNAPSHOT$ ]]
then
  ARTIFACTORY="https://digitalasset.artifactoryonline.com/digitalasset/webapp/#/artifacts/browse/tree/General/libs-snapshot-local/org/hyperledger/secp-binding/$VERSION"
else
  ARTIFACTORY="https://digitalasset.artifactoryonline.com/digitalasset/webapp/#/artifacts/browse/tree/General/libs-release-local/org/hyperledger/secp-binding/$VERSION"
fi

/bin/echo "ARTIFACTORY $ARTIFACTORY"

CURRENT_COMMIT_SHA=$(git log -n 1 --pretty=format:"%h")
echo "CURRENT_COMMIT_SHA: $CURRENT_COMMIT_SHA"

CURRENT_BRANCH="$(git branch -a --contains $CURRENT_COMMIT_SHA --merged | grep "remotes/origin/" | cut -d "/" -f "3" | tr '\n' '/')"
echo "CURRENT_BRANCH: $CURRENT_BRANCH"

git log --pretty=format:"Building commit <a href=\"https://github.com/DACH-NY/secp-binding/commit/%H\" title=\"%s\">[$CURRENT_BRANCH (%h)]</a> -&gt; <a href=\"$ARTIFACTORY\"><b>$VERSION</b></a> - %an eof building commit" -n 1



