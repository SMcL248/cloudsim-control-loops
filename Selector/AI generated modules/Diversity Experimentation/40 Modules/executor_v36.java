package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v36 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v36] ",
                    "malformed payload, expected {hostId}");
            return false;
        }

        int hostId = payload[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v36] ",
                    "aborting power-down, unresolved host reference for id " + hostId);
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v36] ",
                    "skipping power-down, host " + hostId + " is already powered down");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v36] ",
                    "skipping power-down, host " + hostId + " is failed or permanently dead and not a normal power target");
            return false;
        }

        actionSpace.requestHostPowerDown(host);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v36] ",
                "state check passed, issued requestHostPowerDown host=" + hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Power down a host";
    }

    @Override
    public int inputGuid() {
        return 3010;
    }
}
