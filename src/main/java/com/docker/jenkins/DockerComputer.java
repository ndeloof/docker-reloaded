package com.docker.jenkins;

import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerComputer extends SlaveComputer {

    protected String container;

    public DockerComputer(DockerAgent slave) {
        super(slave);
    }

    @Override
    public DockerComputerLauncher getLauncher() {
        return (DockerComputerLauncher) super.getLauncher();
    }

    protected void removeExecutor(Executor e) {
        setAcceptingTasks(false);
        try {
            disconnect(new OfflineCause.UserCause(null, "stopping container"));
            Jenkins.get().removeNode(getNode());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        super.removeExecutor(e);
    }
}
