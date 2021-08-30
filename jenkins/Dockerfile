FROM jenkins/jenkins

USER root

# https://docs.docker.com/compose/install/#install-compose-on-linux-systems
RUN curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
RUN chmod +x /usr/local/bin/docker-compose
RUN ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
