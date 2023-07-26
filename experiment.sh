#!/bin/bash

rm -rf *.log
echo "cache cleaning..:"
echo 3 > /proc/sys/vm/drop_caches
echo "cache cleaning is complete"

#rm -rf /tmp/zookeeper/*
#rm -rf /tmp/kafka-logs_3.2.3/stream_*
#rm -rf /tmp/kafka-streams/stream_wordcount_application/*

#$KAFKA_HOME/bin/kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic wordcount3
#$KAFKA_HOME/bin/kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic wordcount-output3

#$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic wordcount3 --replication-factor 1 --config message.timestamp.type=LogAppendTime
#$KAFKA_HOME/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic wordcount-output3 --replication-factor 1 --config message.timestamp.type=LogAppendTime

echo "All kafka topic settings are complete"
java -cp target/dcclab-streamwordcount.jar com.dcclab.examples.streams.wordcount.WordCountExample
./kill_example.sh
