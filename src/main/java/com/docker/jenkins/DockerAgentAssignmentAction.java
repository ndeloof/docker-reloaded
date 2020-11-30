package com.docker.jenkins;

import hudson.model.InvisibleAction;
import hudson.model.Label;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.queue.SubTask;

public class DockerAgentAssignmentAction extends InvisibleAction implements LabelAssignmentAction {
    
    private final String assignedNodeName;

    public DockerAgentAssignmentAction(String assignedNodeName) {
        this.assignedNodeName = assignedNodeName;
    }

    @Override
    public Label getAssignedLabel(SubTask task) {
        return Label.get(assignedNodeName);
    }
}