package com.docker.jenkins;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;

import java.io.IOException;

@Extension
public class DockerQueueListener extends QueueListener {

    @Override
    public void onEnterBuildable(final Queue.BuildableItem item) {
        if (item.task instanceof Job) {
            Job job = (Job) item.task;
            try {
                Node node = prepareExecutorFor(job);
                DockerAgentAssignmentAction action = new DockerAgentAssignmentAction(node.getNodeName());
                item.addAction(action);

                Computer.threadPoolForRemoting.submit(() -> {
                    try {
                        Jenkins.get().addNode(node);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Descriptor.FormException | IOException e) {
                e.printStackTrace();
            }


        }
    }

    private Node prepareExecutorFor(final Job job) throws Descriptor.FormException, IOException {
        Node node = new DockerAgent("Container for " + job.getName() + "#" + job.getNextBuildNumber());
        return node;
    }

    @Override
    public void onLeft(Queue.LeftItem item) {
        if (item.isCancelled()) {
        }
    }
}