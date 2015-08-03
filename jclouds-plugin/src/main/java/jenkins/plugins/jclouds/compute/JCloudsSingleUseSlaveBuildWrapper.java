package jenkins.plugins.jclouds.compute;

import java.io.IOException;
import java.util.Map;

import org.jclouds.compute.ComputeService;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.slaves.OfflineCause;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import shaded.com.google.common.base.Strings;

public class JCloudsSingleUseSlaveBuildWrapper extends BuildWrapper {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(JCloudsSingleUseSlaveBuildWrapper.class.getName());

    @DataBoundConstructor
    public JCloudsSingleUseSlaveBuildWrapper() {

    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener) {
        LOGGER.info("Start single-use slave Extension setup");

        if (JCloudsComputer.class.isInstance(build.getExecutor().getOwner())) {
            // Get current running node
            final JCloudsComputer c = (JCloudsComputer) build.getExecutor().getOwner();
            final JCloudsCloud jcloudsCloud = JCloudsCloud.getByName(c.getCloudName());
            final JCloudsSlave jcloudsSlave = c.getNode();
            final String nodeId = jcloudsSlave.getNodeId();
            LOGGER.info("Get slave: " + jcloudsSlave.getDisplayName() + " nodeId " + nodeId);
            LOGGER.info("Get slave user id: " + jcloudsSlave.getUserId());
            final ComputeService computeService = jcloudsCloud.getCompute();

            // Rename that running node with job name and user name
            String buildTag = (String) build.getEnvVars().get("BUILD_TAG");
            final String nodeName;
            String buildUser = (String) build.getEnvVars().get("BUILD_USER");
            if (Strings.isNullOrEmpty(buildUser)) {
                nodeName = buildTag.toLowerCase();
            } else {
                nodeName = (buildTag + "-" + buildUser).toLowerCase();
            }
            LOGGER.info("Rename running node with name: " + nodeName);
            try {
                computeService.renameNode(jcloudsSlave.getNodeId(), nodeName);
            } catch (Exception e) {
                LOGGER.info("Failed to rename the node to: " + nodeName + "\n" + e);
            }

            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.put("JENKINS_NODE_NAME", nodeName);
                }

                @Override
                public boolean tearDown(AbstractBuild build, final BuildListener listener) throws IOException, InterruptedException {
                    // single-use slave, set to offline to prevent from reusing
                    c.setTemporarilyOffline(true, OfflineCause.create(Messages._OneOffCause()));

                    String slavePostAction = (String) build.getEnvVars().get("slavePostAction");
                    if (!Strings.isNullOrEmpty(slavePostAction)) {
                        switch (slavePostAction) {
                        case InstancePostAction.OFFLINE_SLAVE_JOB_DONE:
                            LOGGER.info("Offline parameter set: Offline slave " + jcloudsSlave.getDisplayName()
                                    + "(" + nodeId + ") when job done");
                            jcloudsSlave.setOverrideRetentionTime(-1);
                            jcloudsSlave.setPendingDelete(true);
                            break;
                        case InstancePostAction.SUSPEND_SLAVE_JOB_FAILED:
                            Result buildResult = build.getResult();
                            if (buildResult == Result.UNSTABLE || buildResult != Result.FAILURE) {
                                LOGGER.info("Suspend slave " + jcloudsSlave.getDisplayName()
                                        + "(" + nodeId + ") when job failed");
                                jcloudsSlave.setOverrideRetentionTime(-1);
                                //computeService.suspendNode(nodeId);
                            }
                            break;
                        case InstancePostAction.SUSPEND_SLAVE_JOB_DONE:
                            LOGGER.info("Suspend slave " + jcloudsSlave.getDisplayName()
                                    + "(" + nodeId + ") when job done");
                            jcloudsSlave.setOverrideRetentionTime(-1);
                            //computeService.suspendNode(nodeId);
                            break;
                        default:
                            //Nothing to do if to destroy the node, let it do by cleanup thread
                            LOGGER.info("To delete slave " + jcloudsSlave.getDisplayName()
                                    + "(" + nodeId + ") when job done");
                        }
                    }
                    return true;
                }
            };
        } else {
            return new Environment() {
            };
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public String getDisplayName() {
            return "JClouds Single Slave Plugin-Ex";
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return true;
        }

    }
}
