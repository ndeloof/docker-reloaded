package com.docker.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerAgent extends Slave {

    public static final String ROOT = "/home/jenkins/agent/";

    public DockerAgent(String name) throws Descriptor.FormException, IOException {
        super(name, ROOT, new DockerComputerLauncher());
        setNumExecutors(1);
        setMode(Mode.EXCLUSIVE);
        setRetentionStrategy(RetentionStrategy.NOOP);
    }

    @Override
    public Computer createComputer() {
        return new DockerComputer(this);
    }

    @Override
    public Launcher createLauncher(TaskListener listener) {
        final DockerComputer c = (DockerComputer) getComputer();
        if (c == null) {
            listener.error("Issue with creating launcher for agent " + name + ". Computer has been disconnected");
            return new Launcher.DummyLauncher(listener);
        }
        return c.createLauncher(listener);
    }

    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "Docker Agent";
        }
    }
}
