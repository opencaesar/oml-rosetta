# Optional Docker registry cache.
# For example, if https://mycompany.com:10000 is a Docker registry cache, 
# then build like this: docker build --build-arg "image_cache=mycompany.com:10000/" ...
ARG image_cache=""
FROM ${image_cache}eclipse-temurin:11-jdk-jammy

RUN apt-get update

RUN apt-get install -y --fix-missing \
    jq \
    curl \
    git \
    git-lfs \
    gnupg \
    openssl \
    libxml2-dev \
    rsync \
    time \
    tzdata \
    wget

# For running Rosetta inside a container, displaying to a virtual X11 framebuffer, which is scrapped to a no-VNC server accessed from a web browser.
RUN apt-get install -y --fix-missing \
  fluxbox \
  net-tools \
  supervisor \
  novnc \
  x11vnc \
  xvfb \
  libswt-gtk-4-jni \
  libswt-gtk-4-java \
  libswt-webkit-gtk-4-jni

RUN apt-get install -y --fix-missing \
    python3 \
    pip \
    r-base 

# https://github.com/cli/cli/blob/trunk/docs/install_linux.md
RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
RUN chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg 
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null
RUN apt-get update
RUN apt-get install -y gh

RUN pip3 install bikeshed && bikeshed update

# This list depends on the project's analysis reports.
# Note that installation failures are ignored unfortunately!
# If this becomes a significant issue, consider a technique like
# the following to verify that packages have been successfully installed.
# https://github.com/shrektan/rdocker/tree/master/r-production

# create simple html tables
RUN /usr/bin/Rscript -e "install.packages('htmlTable')"
# create more complex html tables
RUN /usr/bin/Rscript -e "install.packages('huxtable')"
# adds distinct function
RUN /usr/bin/Rscript -e "install.packages('dplyr')"
# functions for d3 graphics
RUN /usr/bin/Rscript -e "install.packages('r2d3')"
# graph library
RUN /usr/bin/Rscript -e "install.packages('igraph')"
# basic assertions
RUN /usr/bin/Rscript -e "install.packages('assertthat')"
# compareDF for data compare
RUN /usr/bin/Rscript -e "install.packages('compareDF')"
# Converts R object into JSON objects and vice-versa.
RUN /usr/bin/Rscript -e "packageurl <- 'https://cran.r-project.org/src/contrib/Archive/rjson/rjson_0.2.20.tar.gz'; install.packages(packageurl, repos=NULL, type='source')"

ARG rosetta_version="1.3.3"

RUN wget -q -O /tmp/rosetta.zip \
https://github.com/opencaesar/oml-rosetta/releases/download/${rosetta_version}/rosetta-${rosetta_version}-linux.gtk.x86_64.zip

RUN apt-get install -y \
    gradle \
    firefox \
    xclip \
    xdotool

COPY app /app

RUN mkdir /app/bin

# Install the Coursier CLI, cs, a Maven artifact resolver
# https://get-coursier.io/docs/cli-installation
RUN curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > /app/bin/cs
RUN chmod +x /app/bin/cs

RUN /app/bin/cs setup -y

# Fetch Maven artifacts for frequently-used OMl ecosystem utilities

# https://github.com/opencaesar/oml-tools
RUN /app/bin/cs fetch "io.opencaesar.oml:oml-bikeshed-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.oml:oml-convert-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.oml:oml-merge-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.oml:oml-validate-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.oml:oml-velocity-gradle:[1,2)"

# https://github.com/opencaesar/owl-adapter
RUN /app/bin/cs fetch "io.opencaesar.adapters:oml2owl-gradle:[1,2)"

# https://github.com/opencaesar/ecore-tools
RUN /app/bin/cs fetch "io.opencaesar.ecore:ecore-bikeshed-gradle:[0,1)"

# https://github.com/opencaesar/owl-tools
RUN /app/bin/cs fetch "io.opencaesar.owl:owl-fuseki-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.owl:owl-load-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.owl:owl-query-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.owl:owl-reason-gradle:[1,2)"
RUN /app/bin/cs fetch "io.opencaesar.owl:owl-shacl-fuseki-gradle:[1,2)"

# Fetch Maven artifacts for frequently-used OMl vocabularies

# https://github.com/opencaesar/core-vocabularies
RUN /app/bin/cs fetch "io.opencaesar.ontologies:core-vocabularies:[3,4)"

# https://github.com/opencaesar/imce-vocabularies
RUN /app/bin/cs fetch "io.opencaesar.ontologies:imce-vocabularies:[3,4)"

# https://github.com/opencaesar/metrology-vocabularies
RUN /app/bin/cs fetch "io.opencaesar.ontologies:metrology-vocabularies:[5,6)"

# https://github.com/opencaesar/provenance-vocabularies
RUN /app/bin/cs fetch "io.opencaesar.ontologies:provenance-vocabularies:[2,3)"

# https://github.com/opencaesar/eclipse-vocabularies
# Not yet published
#RUN /app/bin/cs fetch "io.opencaesar.ontologies:eclipse-bpmn2:[1,2)"
#RUN /app/bin/cs fetch "io.opencaesar.ontologies:eclipse-uml2:[1,2)"

RUN mkdir -p /app/rosetta

RUN cd /app && unzip /tmp/rosetta.zip && rm /tmp/rosetta.zip

ENV HOME=/root \
    DEBIAN_FRONTEND=noninteractive \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8 \
    LC_ALL=C.UTF-8 \
    DISPLAY=:0.0 \
    DISPLAY_WIDTH=1600 \
    DISPLAY_HEIGHT=1000 

CMD [ "supervisord", "-c", "/app/supervisord.conf" ]

EXPOSE 8080

