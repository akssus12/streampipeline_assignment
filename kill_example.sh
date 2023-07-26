#!/bin/bash

ps -elf | grep WordCountExample | awk '{print $4}' > temp.txt
#ps -elf | grep data-generator | awk '{print $4}' > temp_generator

cat temp.txt | while read line || [[ -n "$line" ]];
do
	kill -9 $line
done

#cat temp_generator | while read line || [[ -n "$line" ]];
#do
#    kill -9 $line
#done
