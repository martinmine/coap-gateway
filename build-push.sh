#!/bin/bash
gradle clean
gradle installDist
docker build -t martinmine/coap-gateway .
docker push martinmine/coap-gateway
