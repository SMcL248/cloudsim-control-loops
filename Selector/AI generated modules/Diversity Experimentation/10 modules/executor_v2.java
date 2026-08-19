package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Variant angle: requestVmMigration, guarded with a feasibility check (target host
// not failed, has room for the VM per canMigrateGuestToHost) and a redundancy check
// (skip if the VM is already mid-migration) before firing the request.
public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(now, ": [executor_v2] REJECTED malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(now, ": [executor_v2] REJECTED migration, VM ", vmId, " or host ", targetHostId, " could not be resolved");
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(now, ": [executor_v2] SKIPPED migration, VM ", vmId, " is already migrating");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(now, ": [executor_v2] SKIPPED migration, target host ", targetHostId, " is failed or dead");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(now, ": [executor_v2] SKIPPED migration, target host ", targetHostId, " lacks capacity for VM ", vmId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(now, ": [executor_v2] ATTEMPTED requestVmMigration vm=", vmId, " targetHost=", targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "migrate VM to target host";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
