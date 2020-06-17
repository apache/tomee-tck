package org.apache.openejb.cts.deploy;

import com.sun.ts.lib.porting.DeploymentInfo;
import com.sun.ts.lib.porting.TSDeploymentException;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Properties;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;

public interface TSDeploymentInterface2 {
    void init(PrintWriter var1);

    String getClientClassPath(TargetModuleID[] var1, DeploymentInfo var2, DeploymentManager var3) throws TSDeploymentException;

    Object getDeploymentPlan(DeploymentInfo var1) throws TSDeploymentException;

    void createConnectionFactory(TargetModuleID[] var1, Properties var2) throws TSDeploymentException;

    void removeConnectionFactory(TargetModuleID[] var1, Properties var2) throws TSDeploymentException;

    String getAppClientArgs(Properties var1);

    Hashtable getDependentValues(DeploymentInfo[] var1);

    Target[] getTargetsToUse(Target[] var1, DeploymentInfo var2);

    void postDistribute(ProgressObject var1);

    void postStart(ProgressObject var1);

    void postStop(ProgressObject var1);

    void postUndeploy(ProgressObject var1);
}
