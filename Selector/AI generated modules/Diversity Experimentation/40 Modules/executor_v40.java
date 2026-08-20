package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v40 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v40] ",
                    "malformed payload, expected {hostId}");
            return false;
        }

        int hostId = payload[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v40] ",
                    "unresolved host reference for id " + hostId);
            return false;
        }

        if (actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v40] ",
                    "skipping power-up, host " + hostId + " already has a power-up in progress");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v40] ",
                    "skipping power-up, host " + hostId + " is permanently dead");
            return false;
        }

        actionSpace.requestHostPowerUp(host);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v40] ",
                "in-progress check passed, issued requestHostPowerUp host=" + hostId);
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
