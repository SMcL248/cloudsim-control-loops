package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// GUID 3011 -- requestHostPowerUp
// Strategy: capacity-expansion gatekeeper. Powering up a host is only useful
// for throughput if it actually brings new placement capacity online, so
// this executor refuses to fire on a permanently dead host (which can never
// recover) and skips redundant requests against a host that is already on
// or already powering up.
public class executor_v10 implements Executor<int[]> {

    private static final int GUID = 3011;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] Malformed payload for requestHostPowerUp, expected 1 int, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] No prescribed action this cycle.");
            return false;
        }

        int hostId = action[0];
        HostEntity host = actionSpace.getHostById(hostId);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] Unknown host reference " + hostId + ", aborting power-up.");
            return false;
        }
        if (actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] Host " + hostId + " is permanently dead, powering it up cannot add capacity, aborting.");
            return false;
        }
        if (!actionSpace.isHostPoweredDown(host) && !actionSpace.isHostPoweringUp(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] Host " + hostId + " is already on, skipping redundant power-up.");
            return false;
        }

        actionSpace.requestHostPowerUp(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] Requested power-up of host " + hostId + " to expand available capacity.");
        return true;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Power up a recoverable host to expand available placement capacity for VMs and cloudlets";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
