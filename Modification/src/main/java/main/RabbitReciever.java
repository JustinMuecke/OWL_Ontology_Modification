package main;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class RabbitReciever {
    private final static String INPUT_QUEUE = "Modification";  // Input queue name
    private final static String OUTPUT_QUEUE = "Prepocessing"; // Output queue name
    private final static String RABBITMQ_HOST = "localhost";  // RabbitMQ host
    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        try(Connection connection = factory.newConnection()){
            Channel channel = connection.createChannel();

            channel.queueDeclare(INPUT_QUEUE, true, false, false, null);
            channel.queueDeclare(OUTPUT_QUEUE, true, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String filepath = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    String newFileName = Main.executeInjection(filepath);
                    channel.basicPublish("", OUTPUT_QUEUE, null, newFileName.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Processed file: " + filepath + " -> " + newFileName);
                } catch (Exception e) {
                    System.err.println("Error processing file " + filepath + ": " + e.getMessage());
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(INPUT_QUEUE, false, deliverCallback, consumerTag -> {});
            while(true){
                Thread.sleep(1000);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
