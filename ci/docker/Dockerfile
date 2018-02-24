FROM openjdk:8u121-jdk

MAINTAINER Juno Composer Team

RUN apt-get update && \
    apt-get --assume-yes install git curl sudo && \
    curl -sL https://deb.nodesource.com/setup_7.x | bash - && apt-get install --assume-yes nodejs

# `aapt` Android SDK build-tool is needed
# v26.0.1, https://issuetracker.google.com/issues/64292349
ENV ANDROID_SDK_URL "https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip"
ENV ANDROID_SDK_FILE_NAME "android-sdk.zip"

ENV ANDROID_HOME /opt/android-sdk-linux
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:${ANDROID_HOME}/platform-tools

RUN \
  mkdir -p $ANDROID_HOME && \
  curl $ANDROID_SDK_URL --progress-bar --location --output $ANDROID_SDK_FILE_NAME && \
  unzip $ANDROID_SDK_FILE_NAME -d $ANDROID_HOME && \
  rm $ANDROID_SDK_FILE_NAME

# Download required parts of Android SDK (separate from Android SDK layer).

ENV ANDROID_SDK_COMPONENTS_REVISION 2017-10-25-15-22
ENV ANDROID_SDK_INSTALL_COMPONENT "echo \"y\" | \"$ANDROID_HOME\"/tools/bin/sdkmanager --verbose"

RUN \
  echo "Android SDK packages revision $ANDROID_SDK_COMPONENTS_REVISION" && \
  eval $ANDROID_SDK_INSTALL_COMPONENT '"build-tools;27.0.3"'

# Entrypoint script will allow us run as non-root in the container.
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
