package com.docker.jenkins;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution;

import java.io.IOException;

@Extension
public class DockerQueueListener extends QueueListener {

    @Override
    public void onEnterBuildable(final Queue.BuildableItem item) {
        final Queue.Task task = item.task;
        try {
            final Node node = prepareExecutorFor(item, task);
            if (node != null) {
                DockerAgentAssignmentAction action = new DockerAgentAssignmentAction(node.getNodeName());
                item.addAction(action);

                Computer.threadPoolForRemoting.submit(() -> {
                    try {
                        Jenkins.get().addNode(node);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Descriptor.FormException | IOException e) {
            e.printStackTrace();
        }
    }

    private Node prepareExecutorFor(Queue.BuildableItem item, final Queue.Task task) throws Descriptor.FormException, IOException {
        if (task instanceof Job) {
            final Job job = (Job) task;
            return new DockerAgent("Container for " + job.getFullDisplayName() + "#" + item.getId());
        }
        if (task instanceof ExecutorStepExecution.PlaceholderTask) {
            ExecutorStepExecution.PlaceholderTask placeholder = (ExecutorStepExecution.PlaceholderTask)task;
            String label = placeholder.getAssignedLabel().getName();
            if (label.startsWith("docker:")) {
                System.out.println("youpi" + label.substring(7));
            }
            return prepareExecutorFor(item, placeholder.getOwnerTask());
        }
        return null;
    }

    @Override
    public void onLeft(Queue.LeftItem item) {
        if (item.isCancelled()) {
        }
    }
}