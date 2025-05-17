#!/bin/bash

docker pull zaproxy/zap-stable
docker run -i zaproxy/zap-stable zap-baseline.py -t "http://146.190.102.157/" -l PASS > zap_baseline_report.txt

echo $? > /dev/null