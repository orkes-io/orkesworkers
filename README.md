# Sample workflows using Netflix Conductor

## These demos are a work in progress, and under continuous improvement.

## Running your first worker

The [Running your first worker](https://orkes.io/content/docs/getting-started/run/running-first-worker) tutorial will walk you through running your first worker.  This will use the SimpleWorker.java and OrkesWorkerApplication.java

## Creating an image processing workflow
### Run locally
Once you have set up [Conductor locally](https://orkes.io/content/docs/getting-started/install/running-locally), there are several image processing workflows you can run.

1. First image processing workflow. The [tutorial](https://orkes.io/content/blog/image-processing-workflow-with-conductor) will walk you through setting up dastks and a workflow.  Once it is up and running, you can send an image and dimensions - and receive a link to the modified image on S3.
2. Image processing with FORKs.  Using a [Conductor Fork](https://orkes.io/content/blog/image-processing-multiple-images-forks) allows us to run  processes in parallel - in this case creating a jpg and webp image.
3. Image processing with FORKs and Sub_workflows.  This example substitues the 2 tasks for the JPEG creation in example 2 with a subworkflow.