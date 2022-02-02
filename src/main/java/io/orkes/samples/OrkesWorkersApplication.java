package io.orkes.samples;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import lombok.extern.slf4j.Slf4j;
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

    private final Environment env;

    public OrkesWorkersApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) throws IOException {
        loadExternalConfig();
        SpringApplication.run(OrkesWorkersApplication.class, args);
    }

    @Bean
    public TaskClient taskClient() {
        log.info("Conductor Server URL: {}", env.getProperty("conductor.server.url"));
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI(env.getProperty("conductor.server.url"));
        return taskClient;
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
            }else {
                log.warn("Ignoring {} since it does not exist", configFile);
            }
        }
    }

}
