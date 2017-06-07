package jenkins.plugins.debpackager;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.ParametersAction;
import hudson.tasks.Builder;

import org.kohsuke.stapler.DataBoundConstructor;

public class RepreproBuilder extends Builder {

    private final String distribution;
    private final String component;

    @DataBoundConstructor
    public RepreproBuilder(String distribution, String component) {
      this.distribution = distribution;
      this.component = component;
    }

    public String getDistribution() {
      return distribution;
    }

    public String getComponent() {
      return component;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        int retval = -1;
        String distributionSub = getParameterString(distribution, build, listener);
        String componentSub = getParameterString(component, build, listener);
        listener.getLogger().println("Deb Packager - adding package to reprepro...");
        try {
            retval = launcher
                    .launch()
                    .cmds(new String[] { "reprepro", "--keepunreferencedfiles", "-Vb",
                            "/var/lib/reprepro", "--component", componentSub, "includedeb", distributionSub, ".packaged.deb" })
                    .envs(build.getEnvironment(listener)).stdout(listener)
                    .pwd(build.getWorkspace()).join();
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }

        listener.getLogger().println("Deb Packager - finished reprepro");
        return retval == 0;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Deb Packager - Reprepro";
        }
    }

    private String getParameterString(String original, AbstractBuild<?, ?> build,
            BuildListener listener) {
        ParametersAction parameters = build.getAction(ParametersAction.class);
        if (parameters != null) {
            original = parameters.substitute(build, original);
        }

        try {
            original = Util.replaceMacro(original, build.getEnvironment(listener));
        } catch (Exception e) {
        }

        return original;
    }

}
