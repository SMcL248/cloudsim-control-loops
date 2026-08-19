package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v10 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] rejected malformed payload, expected 1 int");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] rejected, unresolved hostId=", hostId);
            return false;
        }

        try {
            actionSpace.requestHostPowerDown(host);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] requestHostPowerDown threw, hostId=", hostId);
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] attempted requestHostPowerDown, hostId=", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "power down host";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
