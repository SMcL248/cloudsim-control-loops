package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Executor for ActionSpace.requestVmMigration - GUID suffix 02.
public class executor_v2 implements Executor<int[]> {

    private static final int GUID = 3002;
    private static final String SEMANTIC =
        "requestVmMigration: relocate a VM onto a different target host. Payload {vmId, targetHostId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v2] rejected - payload must have exactly 2 entries {vmId, targetHostId}.");
            return false;
        }

        if (actions[0] == -1 && actions[1] == -1) {
            Log.printlnConcat(now, ": [executor_v2] no-op sentinel received, nothing to migrate.");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(now, ": [executor_v2] rejected - VM or target host could not be resolved (vmId=", vmId, ", targetHostId=", targetHostId, ").");
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(now, ": [executor_v2] rejected - VM ", vmId, " is already mid-migration.");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost) || actionSpace.isHostPoweredDown(targetHost)) {
            Log.printlnConcat(now, ": [executor_v2] rejected - target host ", targetHostId, " is failed, dead or powered down and cannot accept a migration.");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(now, ": [executor_v2] rejected - target host ", targetHostId, " lacks sufficient headroom for VM ", vmId, ".");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(now, ": [executor_v2] requested migration of VM ", vmId, " to host ", targetHostId, ".");
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
