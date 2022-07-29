package io.orkes.samples.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class TrioD implements Worker {
    @Override
    public String getTaskDefName() {
        return "trio_d";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("d", "d");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
