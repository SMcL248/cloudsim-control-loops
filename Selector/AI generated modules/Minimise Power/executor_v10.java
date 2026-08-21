package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Executor for ActionSpace.requestHostPowerUp - GUID suffix 11.
public class executor_v10 implements Executor<int[]> {

    private static final int GUID = 3011;
    private static final String SEMANTIC =
        "requestHostPowerUp: power on a currently-off host, briefly incurring an elevated power-spike model. Payload {hostId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v10] rejected - payload must have exactly 1 entry {hostId}.");
            return false;
        }

        if (actions[0] == -1) {
            Log.printlnConcat(now, ": [executor_v10] no-op sentinel received, no power-up requested.");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(now, ": [executor_v10] rejected - host ", hostId, " could not be resolved.");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(now, ": [executor_v10] rejected - host ", hostId, " is permanently dead and cannot be powered up.");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(now, ": [executor_v10] rejected - host ", hostId, " is not currently powered down.");
            return false;
        }

        if (actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(now, ": [executor_v10] rejected - host ", hostId, " is already in the process of powering up.");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(now, ": [executor_v10] requested power-up of host ", hostId, ".");
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
