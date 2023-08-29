from conductor.client.http.models.task import Task
from conductor.client.http.models.task_result import TaskResult
from conductor.client.http.models.task_exec_log import TaskExecLog
from conductor.client.http.models.task_result_status import TaskResultStatus

from workers.utils.ocr import ocr

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

def python_ocr(task: Task) -> TaskResult:
    ocr_result = ocr(task.input_data["image_url"])
    task_result = to_task_result(task)
    task_result.logs.append(
        TaskExecLog(
            log=f'Completed OCR'
        )
    )
    task_result.status = TaskResultStatus.COMPLETED
    task_result.add_output_data('result', ocr_result)
    return task_result


def to_task_result(task: Task) -> TaskResult:
    return TaskResult(
        task_id=task.task_id,
        workflow_instance_id=task.workflow_instance_id,
        worker_id=socket.gethostname(),
        logs=[],
    )
