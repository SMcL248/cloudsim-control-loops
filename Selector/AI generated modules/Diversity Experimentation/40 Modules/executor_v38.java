package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v38 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v38] ",
                    "malformed payload, expected {hostId}");
            return false;
        }

        int hostId = payload[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v38] ",
                    "unresolved host reference for id " + hostId);
            return false;
        }

        actionSpace.requestHostPowerUp(host);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v38] ",
                "issued requestHostPowerUp host=" + hostId + " with no further checks");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Power up a host";
    }

    @Override
    public int inputGuid() {
        return 3011;
    }
}
