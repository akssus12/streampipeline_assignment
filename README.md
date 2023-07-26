To run this example:

0. Build the project with `mvn package`, this will generate an uber-jar with the streams app and all its dependencies.
    When you modify the source code, you have to re-build java code. After successfully building, you can get ~.jar file in target folder.

1. Produce some text to the topic.
    Using HiBench(lass@163.239.14.92), please generate the dataset for counting words.
    
    A. HiBench's dataGenerator ----> kafka broker in 동재's computing machine

    B. Kafka Streams application counts the word occurances in stream manners

    C. During run, you have to get statistics about latency, throughput

2. Run the app..

    java -cp target/uber-kafka-streams-wordcount-1.0-SNAPSHOT.jar com.dcclab.examples.streams.wordcount.WordCountExample

3. Make the output topic
   bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic wordcount-output --replication-factor 1

4. Take a look at the results:

    bin/kafka-console-consumer.sh --topic wordcount-output --from-beginning --bootstrap-server localhost:9092 --property print.key=true
