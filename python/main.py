import sys

# sys.path.insert(1, '../')

import logging
import os
from conductor.client.automator.task_handler import TaskHandler
from workers.workers import python_helloworld, python_ocr
from typing import Dict
from conductor.client.configuration.configuration import Configuration
from conductor.client.configuration.settings.authentication_settings import AuthenticationSettings
from conductor.client.worker.worker import Worker
import threading
import argparse

# logging.disable(level=logging.DEBUG)

KEY = 'conductor_security_client_key_id'
SECRET = 'conductor_security_client_secret'
CONDUCTOR_SERVER_URL = 'conductor_server_url'

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000, help="Port for SSE transport")
    args = parser.parse_args()

    task_handler = start_workers()
    worker_thread = threading.Thread(target=task_handler.join_processes)
    worker_thread.daemon=True
    worker_thread.start()

    from workers.workers import mcp
    mcp.run(transport="sse", port=args.port)


def start_workers() -> TaskHandler:
    task_handler = TaskHandler(
        # Add new workers to the list here....
        # poll interval is in seconds and can be float e.g. 0.5 will mean 500ms
        workers=[
            Worker(
                task_definition_name='python_hello',
                execute_function=python_helloworld,
                poll_interval=1
            ),
            Worker(
                task_definition_name='ocr_task',
                execute_function=python_ocr,
                poll_interval=1
            ),
        ],
        configuration=get_configuration()
    )
    task_handler.start_processes()
    print('started all workers')
    return task_handler


def get_configuration():
    envs = _get_environment_variables()
    params = {
        'server_api_url': envs[CONDUCTOR_SERVER_URL],
        'debug': True,
        'authentication_settings': AuthenticationSettings(
            key_id=envs[KEY],
            key_secret=envs[SECRET]
        )
    }
    return Configuration(**params)


def _get_environment_variables() -> Dict[str, str]:
    envs = {
        KEY: '',
        SECRET: '',
        CONDUCTOR_SERVER_URL: ''
    }
    for env_key in envs.keys():
        value = os.getenv(env_key)
        envs[env_key] = value
    if envs[CONDUCTOR_SERVER_URL] is None:
        raise RuntimeError(f'environment variable not set: {env_key}')
    return envs


if __name__ == '__main__':
    main()
