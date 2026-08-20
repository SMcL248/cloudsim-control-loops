package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v39 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v39] ",
                    "malformed payload, expected {hostId}");
            return false;
        }

        int hostId = payload[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v39] ",
                    "aborting power-up, unresolved host reference for id " + hostId);
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v39] ",
                    "skipping power-up, host " + hostId + " is not currently powered down");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v39] ",
                    "skipping power-up, host " + hostId + " is permanently dead and cannot be revived");
            return false;
        }

        actionSpace.requestHostPowerUp(host);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v39] ",
                "state check passed, issued requestHostPowerUp host=" + hostId);
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
