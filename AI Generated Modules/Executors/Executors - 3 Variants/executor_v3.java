package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * executor_v3 - Defensive Context-Aware Executor
 *
 * Strategy: Guards every edge case independently (null array, wrong length,
 * individual -1 sentinels). Before migrating, queries the VM's current
 * datacenter via ReadSpace and includes it in the log, giving a richer
 * execution trace useful for post-simulation analysis.
 */
public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null) {
            Log.printlnConcat(now, ": [Executor_v3] Null action received. No migration.");
            return false;
        }
        if (actions.length < 2) {
            Log.printlnConcat(now, ": [Executor_v3] Malformed action array (length=", actions.length, "). No migration.");
            return false;
        }
        if (actions[0] == -1 || actions[1] == -1) {
            Log.printlnConcat(now, ": [Executor_v3] Sentinel received (vmId=", actions[0], " hostId=", actions[1], "). No migration.");
            return false;
        }

        int vmId   = actions[0];
        int hostId = actions[1];

        GuestEntity targetVm = null;
        for (GuestEntity g : actionSpace.getVmList()) {
            if (g.getId() == vmId) { targetVm = g; break; }
        }
        if (targetVm == null) {
            Log.printlnConcat(now, ": [Executor_v3] VM ", vmId, " not found. Migration aborted.");
            return false;
        }

        HostEntity targetHost = null;
        for (HostEntity h : actionSpace.getAllHosts()) {
            if (h.getId() == hostId) { targetHost = h; break; }
        }
        if (targetHost == null) {
            Log.printlnConcat(now, ": [Executor_v3] Host ", hostId, " not found. Migration aborted.");
            return false;
        }

        Integer currentDc = actionSpace.getDatacenterFor(targetVm);
        Log.printlnConcat(now, ": [Executor_v3] Migrating VM ", vmId,
                " (current DC=", currentDc, ") to Host ", hostId,
                ". Broker=", actionSpace.getUserId());

        actionSpace.requestVmMigration(targetVm, targetHost);
        return true;
    }

    @Override
    public String inputGuid() {
        return "host-migration-pair";
    }
}
