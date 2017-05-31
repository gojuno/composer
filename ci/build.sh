#!/bin/bash
set -e

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$DIR/.."

pushd "$PROJECT_DIR"

# Files created in mounted volume by container should have same owner as host machine user to prevent chmod problems.
USER_ID=`id -u $USER`

BUILD_COMMAND="set -xe && "
BUILD_COMMAND+="apt-get update && apt-get --assume-yes install git && "

if [ "$USER_ID" == "0" ]; then
    echo "Warning: running as r00t."
else
    BUILD_COMMAND+="apt-get --assume-yes install sudo && "
    BUILD_COMMAND+="groupadd --gid $USER_ID build_user && "
    BUILD_COMMAND+="useradd --shell /bin/bash --uid $USER_ID --gid $USER_ID --create-home build_user && "
    BUILD_COMMAND+="sudo --set-home --preserve-env -u build_user "
fi

BUILD_COMMAND+="/opt/project/gradlew "
BUILD_COMMAND+="--no-daemon --info --stacktrace "
BUILD_COMMAND+="clean build "

if [ "$PUBLISH" == "true" ]; then
    BUILD_COMMAND+="bintrayUpload "
fi

BUILD_COMMAND+="--project-dir /opt/project"

docker run \
--env BINTRAY_USER="$BINTRAY_USER" \
--env BINTRAY_API_KEY="$BINTRAY_API_KEY" \
--env BINTRAY_GPG_PASSPHRASE="$BINTRAY_GPG_PASSPHRASE" \
--volume `"pwd"`:/opt/project \
--rm \
openjdk:8u121-jdk \
bash -c "$BUILD_COMMAND"

popd
