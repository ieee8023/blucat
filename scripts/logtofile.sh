#!/bin/bash

echo #Starting script to log to file

for (( i=1; i <= 500000; i++ )) do


../blucat -csv services | tee -a ../logs/blucat-`date +"%y-%m-%d"`.log

sleep 5m


done


