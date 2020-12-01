package com.docker.jenkins;

import com.docker.jocker.DockerClient;
import com.docker.jocker.io.DockerMultiplexedInputStream;
import com.docker.jocker.model.ContainerCreateResponse;
import com.docker.jocker.model.ContainerSpec;
import com.docker.jocker.model.HostConfig;
import com.docker.jocker.model.HostConfigLogConfig;
import com.docker.jocker.model.Streams;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.Which;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.ArgumentListBuilder;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.docker.jenkins.DockerAgent.ROOT;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerComputerLauncher extends ComputerLauncher {

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (computer instanceof DockerComputer) {
            launch((DockerComputer) computer, listener);
        } else {
            System.err.println(computer);
            throw new IllegalArgumentException("This launcher only can handle DockerComputer");
        }
    }

    private void launch(final DockerComputer computer, TaskListener listener) throws IOException, InterruptedException {
        File file = Which.jarFile(hudson.remoting.Launcher.class);

        listener.getLogger().println("Create Docker container to host the agent ...");
        DockerClient docker = new DockerClient("unix:///var/run/docker.sock");
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
        computer.container = containerId;
        listener.getLogger().printf("Docker container %s created\n", containerId);

        listener.getLogger().printf("Copy %s into agent container\n", file.getName());
        // Inject current slave.jar to ensure adequate version running
        docker.putContainerFile(containerId, ROOT, false, file);

        listener.getLogger().println("Attach to container stdin/stdout");
        Streams streams = docker.containerAttach(containerId, true, true, true, true, false, "", false);
        streams.redirectStderr(listener.getLogger());

        listener.getLogger().println("Start container");
        docker.containerStart(containerId);

        listener.getLogger().println("Container started, create channel on top of stdin/stdout");
        computer.setChannel(streams.stdout(), streams.stdin(), listener.getLogger(), null);
    }

    protected int copy(String containerId, String path, String filename, byte[] content, TaskListener listener) throws IOException, InterruptedException {
        ByteArrayOutputStream out = tar(filename, content);

        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("docker", "cp", "-", containerId + ":" + path);

        return run(args, listener)
                .stdin(new ByteArrayInputStream(out.toByteArray()))
                .join();
    }

    private ByteArrayOutputStream tar(String filename, byte[] content) throws IOException {
        TarEntry entry = new TarEntry(filename);
        entry.setUserId(0);
        entry.setGroupId(0);
        entry.setSize(content.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TarOutputStream tar = new TarOutputStream(out);
        tar.putNextEntry(entry);
        tar.write(content);
        tar.closeEntry();
        tar.close();
        return out;
    }

    private Launcher.ProcStarter run(ArgumentListBuilder args, TaskListener listener) {
        Launcher launcher = new Launcher.LocalLauncher(listener);
        return launcher.launch().cmds(args).stderr(launcher.getListener().getLogger());
    }

}
