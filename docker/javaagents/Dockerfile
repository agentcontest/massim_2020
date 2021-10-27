FROM buildpack-deps:stable-curl

ARG JAVA_VERSION

WORKDIR /usr/javaagents

ENV JABBA_COMMAND "install ${JAVA_VERSION} -o /jdk"
RUN curl -L https://github.com/shyiko/jabba/raw/master/install.sh | bash
ENV JAVA_HOME /jdk
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt-get update
RUN apt-get -y install git maven

RUN git clone https://github.com/agentcontest/massim_2020.git .
RUN mvn clean package

COPY lib/eismassimconfig.json ./javaagents/conf/BasicAgents