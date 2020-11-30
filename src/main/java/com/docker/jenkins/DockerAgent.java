package com.docker.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.slaves.RetentionStrategy;

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
        return new Launcher.DecoratedLauncher(super.createLauncher(listener)) {
            @Override
            public void kill(Map<String, String> modelEnvVars) {
                // NOOP. processes will all get killed as container is stopped
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return "Docker Agent";
        }
    }
}
