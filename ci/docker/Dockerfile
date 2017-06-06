FROM openjdk:8u121-jdk

MAINTAINER Juno Composer Team

RUN apt-get update && \
    apt-get --assume-yes install git curl sudo && \
    curl -sL https://deb.nodesource.com/setup_7.x | bash - && apt-get install --assume-yes nodejs

# Entrypoint script will allow us run as non-root in the container.
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
