package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: idempotency-guarded power-down.
// Powering down destroys hosted VMs and their workloads, so this variant
// only fires when the request will actually change host state: it skips
// hosts that are already powered down, and skips hosts that are failed or
// permanently dead (which never transition to the OFF power model
// anyway), avoiding pointless destructive no-ops.
public class executor_v9 implements Executor<int[]> {

    private static final int INPUT_GUID = 3010;
    private static final String INPUT_SEMANTIC = "power down a host";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 1) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] no-op or malformed payload received, skipping requestHostPowerDown");
            return false;
        }

        int hostId = actions[0];
        HostEntity host = actionSpace.getHostById(hostId);

        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host ", hostId, " no longer exists, skipping power-down");
            return false;
        }

        if (actionSpace.isHostPoweredDown(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host ", hostId, " is already powered down, skipping redundant request");
            return false;
        }

        if (actionSpace.isHostFailed(host) || actionSpace.isHostPermanentlyDead(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] host ", hostId, " is failed or permanently dead and will not transition to OFF, skipping power-down");
            return false;
        }

        int guestsAffected = actionSpace.getVmListForHost(host).size();
        actionSpace.requestHostPowerDown(host);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] requested power-down of host ", hostId, ", evacuating ", guestsAffected, " guest(s) and their workloads");
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
