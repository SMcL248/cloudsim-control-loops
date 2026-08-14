package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Executes the requestHostPowerDown action: powers down a host, destroying any hosted VMs and their workloads.
public class executor_v10 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] invalid payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] cannot resolve host ", hostId, ", aborting");
            return false;
        }

        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] requested power down of host ", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerDown(hostId)";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
