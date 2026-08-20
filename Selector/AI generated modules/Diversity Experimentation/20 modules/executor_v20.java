package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// State-Aware Idempotency Executor for requestHostPowerUp (3011).
// Skips hosts that are not actually powered down, are already mid power-up,
// or are permanently dead, avoiding a futile or redundant power-up request.
public class executor_v20 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ", "Malformed payload, expected {hostId}");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ", "Rejected power up, unknown host=" + hostId);
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ", "Skipped power up, host=" + hostId + " is permanently dead");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host) || actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ", "Skipped power up, host=" + hostId + " is not in a powered-down, ready state");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ", "Powered up host=" + hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestHostPowerUp: power on a host";
    }

    @Override
    public int inputGuid() {
        return 3011;
    }
}
