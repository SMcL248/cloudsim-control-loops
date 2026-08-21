package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: recoverability-gated power-up.
// Powering up a host incurs an energy-hungry power spike, so this variant
// only issues the request for hosts that are actually down and capable of
// recovering: it skips hosts that are not currently powered down (or are
// already mid-power-up), and skips hosts that are permanently dead, since
// those can never come back regardless of the request.
public class executor_v10 implements Executor<int[]> {

    private static final int INPUT_GUID = 3011;
    private static final String INPUT_SEMANTIC = "power up a host";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 1) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] no-op or malformed payload received, skipping requestHostPowerUp");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host ", hostId, " no longer exists, skipping power-up");
            return false;
        }

        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host ", hostId, " is permanently dead and cannot be recovered, skipping power-up");
            return false;
        }

        if (!actionSpace.isHostPoweredDown(host) || actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] host ", hostId, " is not currently powered down or is already powering up, skipping redundant request");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] requested power-up of host ", hostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}
