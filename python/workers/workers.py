from conductor.client.http.models.task import Task
from conductor.client.http.models.task_result import TaskResult
from conductor.client.http.models.task_exec_log import TaskExecLog
from conductor.client.http.models.task_result_status import TaskResultStatus

import socket


def python_helloworld(task: Task) -> TaskResult:
    task_result = to_task_result(task)
    task_result.logs.append(
        TaskExecLog(
            log=f'said hello'
        )
    )
    task_result.status = TaskResultStatus.COMPLETED
    task_result.add_output_data('result', 'hello world')
    return task_result


def to_task_result(task: Task) -> TaskResult:
    return TaskResult(
        task_id=task.task_id,
        workflow_instance_id=task.workflow_instance_id,
        worker_id=socket.gethostname(),
        logs=[],
    )
