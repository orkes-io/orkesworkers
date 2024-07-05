package io.orkes.samples.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class HelloWorld implements Worker {
    @Override
    public String getTaskDefName() {
        return "hello_world";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("hw_response", "Hello World!");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
