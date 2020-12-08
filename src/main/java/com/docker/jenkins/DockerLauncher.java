package com.docker.jenkins;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ExecConfig;
import com.docker.jocker.model.Streams;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamCopyThread;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * A process laucher which wrap commands with `docker exec`
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerLauncher extends Launcher
{
    private final String container;

    public DockerLauncher(TaskListener listener, VirtualChannel channel, String container) {
        super(listener, channel);
        this.container = container;
    }

    @Override
    public Proc launch(Launcher.ProcStarter starter) throws IOException {
        DockerClient docker = new DockerClient("unix:///var/run/docker.sock");
        String execId = docker.containerExec(container, new ExecConfig()
                .attachStderr(true)
                .attachStdout(true)
                .attachStdin(true)
                .cmd(starter.cmds())
                .env(Arrays.asList(starter.envs()))
                .workingDir(starter.pwd().getRemote())
        );

        final Streams streams = docker.execStart(execId, false, false);
        if (starter.stdout() != null) {
            new StreamCopyThread(container+": stdout copier", streams.stdout(), starter.stdout()).start();
            if (starter.stderr() == null) {
                streams.redirectStderr(starter.stdout());
            }
        }
        if (starter.stderr() != null) {
            streams.redirectStderr(starter.stderr());
        }

        if (starter.stdin() != null) {
            new StreamCopyThread(container+": stdout copier", starter.stdin(), streams.stdin()).start();
        }

        return new DockerExec(execId, streams);
    }

    @Override
    public Channel launchChannel(String[] cmd, OutputStream out, FilePath workDir, Map<String, String> envVars) throws IOException, InterruptedException {
        // TODO not sure in which context this is used
        throw new UnsupportedOperationException("please report issue on https://github.com/ndeloof/docker-reloaded");
    }

    @Override
    public void kill(Map<String, String> modelEnvVars) throws IOException, InterruptedException {
        // All processes will be killed as we remove container
    }
}
