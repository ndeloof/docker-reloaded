package com.docker.jenkins;

import com.docker.jocker.DockerClient;
import com.docker.jocker.model.ExecInspectResponse;
import com.docker.jocker.model.Streams;
import hudson.Proc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerExec extends Proc {

    private final String execId;
    private final Streams streams;

    public DockerExec(String execId, Streams streams) throws IOException {
        this.execId = execId;
        this.streams = streams;
    }

    @Override
    public boolean isAlive() throws IOException, InterruptedException {
        final String dockerHost = DockerGlobalConfiguration.get().getDockerHost();
        final DockerClient docker = new DockerClient(dockerHost);
        return docker.execInspect(execId).isRunning();
    }

    @Override
    public void kill() throws IOException, InterruptedException {
        // FIXME exec/kill NOT SUPPORTED
        // Maybe we can `docker exec kill pid`?
        throw new UnsupportedOperationException("please report issue on https://github.com/ndeloof/docker-reloaded");
    }

    @Override
    public int join() throws IOException, InterruptedException {
        // TODO watch docker event and wait for exec die
        final String dockerHost = DockerGlobalConfiguration.get().getDockerHost();
        final DockerClient docker = new DockerClient(dockerHost);while (true) {
            final ExecInspectResponse exec = docker.execInspect(execId);
            if (!exec.isRunning()) {
                return exec.getExitCode();
            }
            Thread.sleep(500);
        }
    }

    @Override
    public InputStream getStdout() {
        try {
            return streams.stdout();
        } catch (IOException e) {
            throw new IllegalStateException("not attached on container's stdout", e);
        }
    }

    @Override
    public InputStream getStderr() {
        return null;
    }

    @Override
    public OutputStream getStdin() {
        try {
            return streams.stdin();
        } catch (IOException e) {
            throw new IllegalStateException("not attached on container's stdin", e);
        }
    }
}
