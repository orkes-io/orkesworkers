package io.orkes.samples;

import com.netflix.conductor.client.worker.Worker;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.TaskClient;
import io.orkes.conductor.client.WorkflowClient;
import io.orkes.conductor.client.automator.TaskRunnerConfigurer;
import io.orkes.samples.workers.AlwaysFailingTaskWorker;
import io.orkes.samples.workers.DHLWorker;
import io.orkes.samples.workers.DynamicTaskArrayPreForkWorker;
import io.orkes.samples.workers.FedExWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
        log.info("Loading Orkes Academy application...");
        loadExternalConfig();
        SpringApplication.run(OrkesWorkersApplication.class, args);
    }

    @Bean
    public OrkesClients orkesClients() {
        String rootUri = env.getProperty(CONDUCTOR_SERVER_URL);
        String key = env.getProperty(CONDUCTOR_CLIENT_KEY_ID);
        String secret = env.getProperty(CONDUCTOR_CLIENT_SECRET);

        if ("_CHANGE_ME_".equals(key) || "_CHANGE_ME_".equals(secret)) {
            log.error("Please provide an application key id and secret");
            throw new RuntimeException("No Application Key");
        }

        ApiClient apiClient = null;

        log.info("Conductor Server URL: {}", rootUri);
        if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret)) {
            log.info("Using Key and Secret to connect to the server");
            apiClient = new ApiClient(rootUri, key, secret);
        } else {
            log.info("setCredentialsIfPresent: Proceeding without client authentication");
            apiClient = new ApiClient(rootUri);
        }
        OrkesClients orkesClients = new OrkesClients(apiClient);
        return orkesClients;
    }

    @Bean
    public TaskClient taskClient(OrkesClients orkesClients) {
        TaskClient taskClient = orkesClients.getTaskClient();
        return taskClient;
    }

    @Bean
    public WorkflowClient workflowClient(OrkesClients orkesClients) {
        WorkflowClient workflowClient = orkesClients.getWorkflowClient();
        return workflowClient;
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
     * Reads properties from the location specified in <code>ORKES_ACADEMY_CONFIG_FILE</code>
     * and sets them as system properties so they override the default properties.
     * <p>
     * Spring Boot property hierarchy is documented here,
     * https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config
     *
     * @throws IOException if file can't be read.
     */
    private static void loadExternalConfig() throws IOException {
        String configFile = System.getProperty("ORKES_ACADEMY_CONFIG_FILE");
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

    @Bean
    public ToolCallbackProvider weatherTools(AlwaysFailingTaskWorker worker, DHLWorker dhlWorker, DynamicTaskArrayPreForkWorker worker2, FedExWorker fedExWorker) {
        return MethodToolCallbackProvider.builder().toolObjects(worker, dhlWorker, worker2, fedExWorker).build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ApplicationContext context) {
        // Get all beans that are annotated with @Component or @Service
        List<Object> toolBeans = context.getBeansOfType(Object.class).values().stream()
                .filter(bean -> {
                    // Check for at least one @Tool method
                    return Arrays.stream(bean.getClass().getMethods())
                            .anyMatch(method -> method.isAnnotationPresent(Tool.class));
                })
                .collect(Collectors.toList());

        return MethodToolCallbackProvider.builder()
                .toolObjects(toolBeans.toArray())
                .build();
    }

}
