package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v5 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Malformed payload for requestHostPowerDown, expected 1 int, action not attempted.");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Host ", hostId, " could not be resolved, power-down not attempted.");
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Host ", hostId, " is already powered down, power-down not attempted.");
            return false;
        }

        int guestCount = actionSpace.getVmListForHost(host).size();
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] Requesting power-down of host ", hostId, ", this will destroy ", guestCount, " hosted VM(s) and their workloads.");
        actionSpace.requestHostPowerDown(host);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerDown";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
