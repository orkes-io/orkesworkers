package io.orkes.samples.external.nats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.OrkesClients;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class ExternalNatsConsumer {


    public static void main(String[] args) {
        String subject = "hello.world.>";
        String stream = "stream-1";
        String consumerName = "pull-consumer-1";

        ExternalNatsConsumer externalNatsConsumer = new ExternalNatsConsumer(subject, consumerName, stream);
        externalNatsConsumer.startConsumer();
    }


    // Nats consumer configuraiton
    private static final int PULL_INITIAL_DELAY = 0; // No delay required as the event handlers fire periodically
    private static final int PULL_PERIOD = 5;  // Keep this low for high load. Ideally make this configurable
    private static final int PULL_BATCH_WAIT_SECONDS = 1;
    public static final String NATS_ENDPOINT = "localhost:4222";
    public static final String CELLBRITE_SIMULATOR = "CELLBRITE_SIMULATOR";


    private static final String CONDUCTOR_SERVER = "http://localhost:8080";
    private static final String KEY = "0b214231-1f02-11f0-93b3-8a59c423699d";
    private static final String SECRET = "AFzsHJpQqJmXrFPckS4h1pONxUMynMuZjESvkTyL0NNXvrvC";

    public static final int BATCH_POLL_CONSUMERS_COUNT = 10;
    public static final long IMAGE_PROCESSING_TIME = 2L; // the sleep time to simulate image processing

    private final String subject;
    private final String stream;
    private final String consumer;

    private Dispatcher dispatcher;
    private final Connection connection;
    private JetStreamSubscription subscription;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;


    @SneakyThrows
    public ExternalNatsConsumer(String subject, String consumer, String stream) {
        this.subject = subject;
        this.stream = stream;
        this.consumer = consumer;
        Options options = connecitonOptions();
        connection = Nats.connect(options);
        dispatcher = connection.createDispatcher();
    }


    @SneakyThrows
    private void startConsumer() {
        JetStreamManagement jetStreamManagement = connection.jetStreamManagement();
        JetStream jetStream = connection.jetStream();
        ConsumerInfo consumerInfo = jetStreamManagement.getConsumerInfo(stream, consumer);
        ConsumerConfiguration consumerConfiguration = consumerInfo.getConsumerConfiguration();

        PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                .stream(stream)
                .configuration(consumerConfiguration)
                .build();

                this.subscription = jetStream.subscribe(subject, pullOptions);

                executor = Executors.newSingleThreadScheduledExecutor();
                scheduledFuture = executor.scheduleAtFixedRate(
                        () -> pullMessagesString(consumer),
                        PULL_INITIAL_DELAY, PULL_PERIOD, TimeUnit.SECONDS);
      }

    private void pullMessagesString (String consumerName) {

        if (subscription == null || !subscription.isActive()) {
            log("Removing Inactive subscription for consumer: " +  consumerName);
            scheduledFuture.cancel(false);
            executor.shutdown();
            return;
        }

        subscription.fetch(BATCH_POLL_CONSUMERS_COUNT, Duration.ofSeconds(PULL_BATCH_WAIT_SECONDS))
                .stream()
                .filter(Message::isJetStream)
                .forEach(this::onMessage);

    }

    private void onMessage(Message message) {
        byte[] data = message.getData();
        String payload = new String(data, StandardCharsets.UTF_8);
        System.out.println("Received message: " + payload);

        Uninterruptibles.sleepUninterruptibly(IMAGE_PROCESSING_TIME, TimeUnit.MILLISECONDS);

       updateTask(payload);

        message.ack();
    }

    private Options connecitonOptions() {
        Options.Builder optionsBuilder = new Options.Builder()
                .server(NATS_ENDPOINT);
        optionsBuilder.connectionName(CELLBRITE_SIMULATOR);
        Options options = optionsBuilder.build();
        return options;
    }


    private void updateTask(String payload)  {
        try {


            Map map = new ObjectMapper().readValue(payload, Map.class);
            Task task = new Task();
            task.setTaskId((String)map.get("taskId"));
            task.setStatus(Task.Status.COMPLETED_WITH_ERRORS);// extract task id from message
            TaskResult taskResult = new TaskResult(task);
            taskResult.setStatus(TaskResult.Status.COMPLETED);
            orkesClients().getTaskClient().updateTask(taskResult);
        } catch (JsonProcessingException e) {
            log("Error while parsing message: " + e.getMessage());
        } catch (Exception e) {
            log("Error while updating task: " + e.getMessage());
        }
    }


    public static OrkesClients orkesClients() {
        String rootUri =CONDUCTOR_SERVER;
        String key = KEY;
        String secret =SECRET;

        ApiClient apiClient = null;

        log("Conductor Server URL: " +  rootUri);
        if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret)) {
            log("Using Key and Secret to connect to the server");
            apiClient = new ApiClient(rootUri, key, secret);
        } else {
            log("setCredentialsIfPresent: Proceeding without client authentication");
            apiClient = new ApiClient(rootUri);
        }
        OrkesClients orkesClients = new OrkesClients(apiClient);
        return orkesClients;
    }





    @SneakyThrows
    public void close() {
        try {
            log("Cleaning up NATS subscription on close");
            if (scheduledFuture != null && executor != null) { //This check is needed as executor would be null for push consumers
                scheduledFuture.cancel(false);
                executor.shutdown();
            }
            if (subscription != null) {
                dispatcher.unsubscribe(subscription);
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            log("Some error occurred closing connection for NATSJetStreamConsumer" + e.getMessage());
            throw e;
        }
    }

    public boolean isClosed() {
        return this.connection == null || !this.connection.getStatus().equals(Connection.Status.CONNECTED);
    }


    private static void log(String s) {
        System.out.println(s);
    }

}
