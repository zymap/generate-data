package org.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

public class Main {
    static class Payment {
        int id;
        String content;
        double price;
    }

    public static void main(String[] args) throws Exception {
        String serviceUrl = System.getenv("serviceUrl");
        System.out.println("serviceUrl: " + serviceUrl);
        String authPlugin = System.getenv("brokerClientAuthenticationPlugin");
        System.out.println("authPlugin: " + authPlugin);
        String authParams = System.getenv("brokerClientAuthenticationParameters");
        System.out.println("authParams: " + authParams);
        String topic = System.getenv("topic");
        System.out.println("topic: " + topic);

        final int num = Integer.parseInt("10000");
        CountDownLatch latch = new CountDownLatch(num);

        PulsarClient client = PulsarClient.builder()
            .serviceUrl(System.getenv("serviceUrl"))
            .authentication(
                AuthenticationFactory.create(authPlugin, authParams)
            )
            .build();

        Producer<Payment> producer = client.newProducer(Schema.AVRO(Payment.class))
            .topic(topic)
            .enableBatching(false)
            .create();
        for (int i = 0; i < num; i++) {
            Payment payment = new Payment();
            payment.id = ThreadLocalRandom.current().nextInt(100);
            payment.content = String.valueOf(ThreadLocalRandom.current().nextInt());
            payment.price = ThreadLocalRandom.current().nextDouble(0, 1000);
            producer.sendAsync(payment).thenRun(latch::countDown);
        }

        latch.await();

        producer.close();
        client.close();

    }
}
