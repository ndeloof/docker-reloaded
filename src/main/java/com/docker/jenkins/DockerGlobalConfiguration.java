package com.docker.jenkins;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

@Extension
public class DockerGlobalConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static DockerGlobalConfiguration get() {
        return ExtensionList.lookupSingleton(DockerGlobalConfiguration.class);
    }

    private String dockerHost = "unix:///var/run/docker.sock";

    public DockerGlobalConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /** @return the currently configured label, if any */
    public String getDockerHost() {
        return dockerHost;
    }

    /**
     * Together with {@link #getDockerHost}, binds to entry in {@code config.jelly}.
     * @param dockerHost the new value of this field
     */
    @DataBoundSetter
    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
        save();
    }

    public FormValidation doCheckLabel(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a label.");
        }
        return FormValidation.ok();
    }

}
