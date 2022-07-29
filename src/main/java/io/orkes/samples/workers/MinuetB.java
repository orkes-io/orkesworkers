package io.orkes.samples.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class MinuetB implements Worker {
    @Override
    public String getTaskDefName() {
        return "minuet_b";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("b", "b!");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
