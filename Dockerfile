FROM openjdk

ADD build/install/http-gateway/lib /usr/local/lib
ADD build/install/http-gateway/bin /usr/local/bin
ADD Californium.properties /

ENTRYPOINT ["http-gateway"]