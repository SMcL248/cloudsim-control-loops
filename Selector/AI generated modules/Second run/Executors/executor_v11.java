package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Executes the requestHostPowerUp action: powers up a previously powered-down host.
public class executor_v11 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] invalid payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] cannot resolve host ", hostId, ", aborting");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] requested power up of host ", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerUp(hostId)";
    }

    @Override
    public int inputGuid() {
        return 3011;
    }
}
