package io.orkes.samples;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootApplication
public class OrkesWorkersApplication {

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(OrkesWorkersApplication.class, args);
    }

    @Bean
    public TaskRunnerConfigurer taskRunnerConfigurer(List<Worker> workersList) {
        log.info("Conductor Server URL: {}", env.getProperty("conductor.server.url"));
        log.info("Starting workers : {}", workersList);
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI(env.getProperty("conductor.server.url"));
        TaskRunnerConfigurer runnerConfigurer = new TaskRunnerConfigurer
                .Builder(taskClient, workersList)
                .withThreadCount(Math.max(1, workersList.size()))
                .build();
        runnerConfigurer.init();
        return runnerConfigurer;
    }

}
