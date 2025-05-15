#!/bin/bash

docker pull zaproxy/zap-stable
docker run -i zaproxy/zap-stable zap-baseline.py -t "https://github.com/CyrussT/nus-iss-group-8" -l PASS > zap_baseline_report.html

echo $? > /dev/null