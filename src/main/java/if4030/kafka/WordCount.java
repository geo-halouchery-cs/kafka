package if4030.kafka;

import java.util.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.io.*;
import java.time.Duration;

public class WordCount {

    private static HashMap<String, String> lexique;
    private static HashMap<String, Integer> countWords;

    public static void main(String[] args) {

        // Initialize the dictionary and word count map
        initDic();
        countWords = new HashMap<>();

        // Set up Kafka consumer properties
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create a Kafka consumer instance
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the 'tagged-words-stream' and 'command-topic' topics
        consumer.subscribe(Arrays.asList("tagged-words-stream", "command-topic"));

        // Continuously poll for new messages
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals("tagged-words-stream")) {
                    String lemme = record.key();
                    String cgram = lexique.get(lemme);
                    int count = countWords.getOrDefault(lemme, 0);
                    countWords.put(lemme, count + 1);
                } else if (record.topic().equals("command-topic") && record.value().equals("END")) {
                    countTopWords();
                }
            }
        }
    }

    public static void initDic() {
        lexique = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("Lexique383.tsv"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\t");
                String lemme = data[2];
                String cgram = data[3];
                lexique.put(lemme, cgram);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void countTopWords() {
        for (String cgram : new HashSet<>(lexique.values())) {
            ArrayList<Map.Entry<String, Integer>> list = new ArrayList<>(countWords.entrySet());
            list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            System.out.println("Top 20 words for cgram " + cgram);
            int count = 0;
            for (Map.Entry<String, Integer> entry : list) {
                if (lexique.get(entry.getKey()).equals(cgram)) {
                    System.out.println(entry.getKey() + " : " + entry.getValue());
                    count++;
                    if (count == 20) {
                        break;
                    }
                }
            }
            System.out.println();
        }
    }
}
