package com.dcclab.examples.streams.wordcount;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.Options;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.CompactionStyle;
import org.rocksdb.CompressionType;

import java.util.Properties;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordCountExample {

    public static class CustomRocksDBConfig implements RocksDBConfigSetter {

        private org.rocksdb.Cache cache = new org.rocksdb.LRUCache(8 * 1024L * 1024L);

        @Override
        public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {
            // Enable RocksDB statistics
            BlockBasedTableConfig tableConfig = (BlockBasedTableConfig) options.tableFormatConfig();
            tableConfig.setBlockCache(cache);
            tableConfig.setBlockSize(16 * 1024L);
            tableConfig.setCacheIndexAndFilterBlocks(true);

            options.setCompactionStyle(CompactionStyle.LEVEL);
            options.setLevel0FileNumCompactionTrigger(4);
            options.setCompressionType(CompressionType.LZ4_COMPRESSION);
            options.setTableFormatConfig(tableConfig);
        }

        @Override
        public void close(final String storeName, final Options options) {
            cache.close();
        }
    }

    public class KafkaMetricsReporter implements Runnable {
        private final KafkaStreams kafkaStreams; // task 별로 latency를 재므로, task 간 평균을 집계할 필요가 있음.
        private double avgFlushTime = 0.0;
        private double avgCompactionTime = 0.0;
        private double avgRecordTime = 0.0;
        private int numOfRecordTasks = 0;
        private int numOfFlushTasks = 0;
        private int numOfCompactionTasks = 0;

        public KafkaMetricsReporter(KafkaStreams kafkaStreamsNew) {
            LOG.info("Conductor in Reporter Class");
            this.kafkaStreams = kafkaStreamsNew;
        }

        @Override
        public void run() {
            Map<MetricName, ? extends Metric> metrics = kafkaStreams.metrics();

            metrics.forEach((metricName, metric) -> {
                // Check client id and metric names , 한 번만 탄다. 그리고 아래 if문은 안 탄다.
                if (!metric.metricValue().equals(0.0) || !metric.metricValue().equals(Double.NEGATIVE_INFINITY)) {
                    if (metricName.name().equals("process-latency-avg")) {
                        avgRecordTime += (double) metric.metricValue();
                        numOfRecordTasks += 1;
                    } else if (metricName.name().equals("compaction-time-avg")) {
                        avgCompactionTime += (double) metric.metricValue();
                        numOfCompactionTasks += 1;
                    } else if (metricName.name().equals("memtable-flush-time-avg")) {
                        avgFlushTime += (double) metric.metricValue();
                        numOfFlushTasks += 1;
                    }
                }
            });
            LOG.info("Number of Flush Tasks {}", numOfFlushTasks);
            LOG.info("Number of Compaction Tasks {}", numOfCompactionTasks);
            LOG.info("Number of Processing Tasks {}", numOfRecordTasks);
            LOG.info("Total latency of Record Tasks {}", avgRecordTime / numOfRecordTasks);
            LOG.info("Total latency of Flush Tasks {}", avgFlushTime / numOfFlushTasks);
            LOG.info("Total latency of Compaction Tasks {}", avgCompactionTime / numOfCompactionTasks);

            avgFlushTime = 0.0;
            avgCompactionTime = 0.0;
            avgRecordTime = 0.0;
            numOfRecordTasks = 0;
            numOfFlushTasks = 0;
            numOfCompactionTasks = 0;
        }

        public void start() {
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this, 0, 500, TimeUnit.MILLISECONDS);
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(WordCountExample.class);

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream_wordcount_application1");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stream_wordcount_group");
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "stream_wordcount_client");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class);
        props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        StreamsBuilder builder = new StreamsBuilder();

        // This is for reset to work. Don't use in production - it causes the app to
        // re-load the state from Kafka on every start

        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        KafkaMetricsReporter k = new WordCountExample().new KafkaMetricsReporter(streams);
        streams.cleanUp();
        k.start();
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}