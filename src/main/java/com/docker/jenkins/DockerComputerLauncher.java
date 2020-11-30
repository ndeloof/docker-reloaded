package com.docker.jenkins;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.slaves.CommandLauncher;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.ArgumentListBuilder;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.docker.jenkins.DockerAgent.ROOT;
import static java.nio.charset.StandardCharsets.UTF_8;

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
        ArgumentListBuilder args = new ArgumentListBuilder()
                .add("docker", "create", "--interactive")

                // We disable container logging to sdout as we rely on this one as transport for jenkins remoting
                .add("--log-driver=none")
                .add("--rm")
                .add("jenkins/agent")
                .add("java")
                .add("-jar").add(ROOT+"slave.jar");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int status = run(args, listener)
                .stdout(out).join();

        if (status != 0) {
            throw new IOException("Failed to create docker image");
        }
        String containerId = out.toString(UTF_8.name()).trim();
        computer.container = containerId;

        // Inject current slave.jar to ensure adequate version running
        copy(containerId, DockerAgent.ROOT, "slave.jar", new Slave.JnlpJar("slave.jar").readFully(), listener);

        args = new ArgumentListBuilder()
                .add("docker", "start")
                .add("--interactive", "--attach", containerId);
        new CommandLauncher(args.toString(), new EnvVars()).launch(computer, listener);
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
