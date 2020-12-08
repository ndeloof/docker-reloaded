package com.docker.jenkins;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ContainerCreateResponse;
import com.docker.jocker.model.ContainerSpec;
import com.docker.jocker.model.HostConfig;
import com.docker.jocker.model.HostConfigLogConfig;
import com.docker.jocker.model.Streams;
import hudson.model.Executor;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.Which;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.docker.jenkins.DockerAgent.ROOT;

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

    public Streams launchContainer(TaskListener listener) throws IOException {
        File file = Which.jarFile(hudson.remoting.Launcher.class);

        listener.getLogger().println("Create Docker container to host the agent ...");
        final String dockerHost = DockerGlobalConfiguration.get().getDockerHost();
        final DockerClient docker = new DockerClient(dockerHost);
        final ContainerCreateResponse created = docker.containerCreate(new ContainerSpec()
                        // --log-driver=none
                        .hostConfig(new HostConfig()
                                .logConfig(new HostConfigLogConfig()
                                        .type(HostConfigLogConfig.TypeEnum.NONE)))
                        .image("jenkins/agent")
                        .cmd(Arrays.asList("java", "-jar", ROOT + file.getName()))
                        // --interactive
                        .attachStdin(true)
                        .attachStdout(true)
                        .attachStderr(true)
                        .openStdin(true)
                        .stdinOnce(true)
                        .tty(false),
                null);

        String containerId = created.getId();
        this.container = containerId;
        listener.getLogger().printf("Docker container %s created\n", containerId);

        listener.getLogger().printf("Copy %s into agent container\n", file.getName());
        // Inject current slave.jar to ensure adequate version running
        docker.putContainerFile(containerId, ROOT, false, file);

        listener.getLogger().println("Attach to container stdin/stdout");
        Streams streams = docker.containerAttach(containerId, true, true, true, true, false, "", false);
        streams.redirectStderr(listener.getLogger());

        listener.getLogger().println("Start container");
        docker.containerStart(containerId);

        return streams;
    }

    protected void removeExecutor(Executor e) {
        setAcceptingTasks(false);
        try {
            disconnect(new OfflineCause.UserCause(null, "stopping container"));
            Jenkins.get().removeNode(getNode());
            final String dockerHost = DockerGlobalConfiguration.get().getDockerHost();
            final DockerClient docker = new DockerClient(dockerHost);
            docker.containerDelete(container, false, false, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        super.removeExecutor(e);
    }


    public DockerLauncher createLauncher(TaskListener listener) {
        final Channel channel = getChannel();
        return new DockerLauncher(listener, channel, container);
    }
}
