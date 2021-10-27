FROM buildpack-deps:stable-curl

ARG JAVA_VERSION
ARG GOAL_URL

ENV JABBA_COMMAND "install ${JAVA_VERSION} -o /jdk"
RUN curl -L https://github.com/shyiko/jabba/raw/master/install.sh | bash
ENV JAVA_HOME /jdk
ENV PATH $JAVA_HOME/bin:$PATH

RUN mkdir /goal
RUN curl -sL ${GOAL_URL} -o /goal/goal.jar

COPY conf/eismassimconfig.json /goal/