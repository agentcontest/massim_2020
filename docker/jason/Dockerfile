FROM buildpack-deps:stable-curl

ARG JASON_JAVA_VERSION
ARG JASON_URL

ENV JABBA_COMMAND "install ${JASON_JAVA_VERSION} -o /jdk"
RUN curl -L https://github.com/shyiko/jabba/raw/master/install.sh | bash
ENV JAVA_HOME /jdk
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt-get update
RUN apt-get -y install gradle