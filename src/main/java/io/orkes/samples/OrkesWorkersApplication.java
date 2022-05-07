package io.orkes.samples;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import io.orkes.conductor.client.http.OrkesClient;
import io.orkes.conductor.client.http.OrkesTaskClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Slf4j
@SpringBootApplication
public class OrkesWorkersApplication {

    private static final String CONDUCTOR_SERVER_URL = "conductor.server.url";
    private static final String CONDUCTOR_CLIENT_KEY_ID = "conductor.security.client.key-id";
    private static final String CONDUCTOR_CLIENT_SECRET = "conductor.security.client.secret";

    private final Environment env;

    public OrkesWorkersApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) throws IOException {
        log.info("Loading Orkes Demo workers application...");
        loadExternalConfig();
        SpringApplication.run(OrkesWorkersApplication.class, args);
    }

    @Bean
    public TaskClient taskClient() {
        String rootUri = env.getProperty(CONDUCTOR_SERVER_URL);
        log.info("Conductor Server URL: {}", rootUri);

        OrkesTaskClient taskClient = new OrkesTaskClient();
        taskClient.setRootURI(rootUri);
        setCredentialsIfPresent(taskClient);

        return taskClient;
    }

    private void setCredentialsIfPresent(OrkesClient client) {
        String keyId = env.getProperty(CONDUCTOR_CLIENT_KEY_ID);
        String secret = env.getProperty(CONDUCTOR_CLIENT_SECRET);

        log.info("Conductor Key: {}", keyId);

        if ("_CHANGE_ME_".equals(keyId) || "_CHANGE_ME_".equals(secret)) {
            log.error("Please provide an application key id and secret");
            throw new RuntimeException("No Application Key");
        }
        if (!StringUtils.isBlank(keyId) && !StringUtils.isBlank(secret)) {
            log.info("setCredentialsIfPresent: Using authentication with access key '{}'", keyId);
            client.withCredentials(keyId, secret);
        } else {
            log.info("setCredentialsIfPresent: Proceeding without client authentication");
        }
    }

    @Bean
    public TaskRunnerConfigurer taskRunnerConfigurer(List<Worker> workersList, TaskClient taskClient) {
        log.info("Starting workers : {}", workersList);
        TaskRunnerConfigurer runnerConfigurer = new TaskRunnerConfigurer
                .Builder(taskClient, workersList)
                .withThreadCount(Math.max(1, workersList.size()))
                .build();
        runnerConfigurer.init();
        return runnerConfigurer;
    }

    /**
     * Reads properties from the location specified in <code>ORKES_WORKERS_CONFIG_FILE</code>
     * and sets them as system properties so they override the default properties.
     * <p>
     * Spring Boot property hierarchy is documented here,
     * https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config
     *
     * @throws IOException if file can't be read.
     */
    private static void loadExternalConfig() throws IOException {
        String configFile = System.getProperty("ORKES_WORKERS_CONFIG_FILE");
        if (!ObjectUtils.isEmpty(configFile)) {
            FileSystemResource resource = new FileSystemResource(configFile);
            if (resource.exists()) {
                Properties properties = new Properties();
                properties.load(resource.getInputStream());
                properties.forEach((key, value) -> System.setProperty((String) key, (String) value));
                log.info("Loaded {} properties from {}", properties.size(), configFile);
            } else {
                log.warn("Ignoring {} since it does not exist", configFile);
            }
        }
        System.getenv().forEach((k, v) -> {
            log.info("System Env Props - Key: {}, Value: {}", k, v);
            if (k.startsWith("conductor")) {
                log.info("Setting env property to system property: {}", k);
                System.setProperty(k, v);
            }
        });
    }

}
