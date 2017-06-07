#!/bin/bash
set -e

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$DIR/.."

pushd "$PROJECT_DIR"

# Files created in mounted volume by container should have same owner as host machine user to prevent chmod problems.
USER_ID=`id -u $USER`

if [ "$USER_ID" == "0" ]; then
    echo "Warning: running as r00t."
fi

docker build -t composer:latest ci/docker

BUILD_COMMAND="set -e && "

BUILD_COMMAND+="echo 'Java version:' && java -version && "
BUILD_COMMAND+="echo 'Node.js version:' && node --version && "
BUILD_COMMAND+="echo 'npm vesion:' && npm --version && "

# Build HTML report app.
BUILD_COMMAND+="echo 'Building HTML report app...' && "
BUILD_COMMAND+="cd /opt/project/html-report && "
BUILD_COMMAND+="rm -rf node_modules && "
BUILD_COMMAND+="npm install && "
BUILD_COMMAND+="npm run build && "
BUILD_COMMAND+="cd /opt/project && "
BUILD_COMMAND+="rm -rf composer/src/main/resources/html-report/ && "
BUILD_COMMAND+="mkdir -p composer/src/main/resources/html-report/ && "
BUILD_COMMAND+="cp -R html-report/build/* composer/src/main/resources/html-report/ && "

# Build Composer.
BUILD_COMMAND+="echo 'Building Composer...' && "
BUILD_COMMAND+="/opt/project/gradlew "
BUILD_COMMAND+="--no-daemon --info --stacktrace "
BUILD_COMMAND+="clean build "

if [ "$PUBLISH" == "true" ]; then
    BUILD_COMMAND+="bintrayUpload "
fi

BUILD_COMMAND+="--project-dir /opt/project"

docker run \
--env LOCAL_USER_ID="$USER_ID" \
--env BINTRAY_USER="$BINTRAY_USER" \
--env BINTRAY_API_KEY="$BINTRAY_API_KEY" \
--env BINTRAY_GPG_PASSPHRASE="$BINTRAY_GPG_PASSPHRASE" \
--volume `"pwd"`:/opt/project \
--rm \
composer:latest \
bash -c "$BUILD_COMMAND"

popd
