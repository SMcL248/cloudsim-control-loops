package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Executor variant implementing requestVmMigration (GUID suffix 02).
// Payload: {vmId, targetHostId}
public class executor_v2 implements Executor<int[]> {

    private static final int EXPECTED_LENGTH = 2;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != EXPECTED_LENGTH) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] rejected payload, expected 2 ints {vmId, targetHostId} but got length ",
                    actions == null ? "null" : actions.length);
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] skipped migration, unknown id(s). vm=", vmId, " host=", targetHostId);
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] skipped migration, vm ", vmId, " is already migrating");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] skipped migration, target host ", targetHostId, " is failed or permanently dead");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] skipped migration, target host ", targetHostId, " lacks capacity for vm ", vmId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] requested migration of vm ", vmId, " to host ", targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmMigration action payload {vmId, targetHostId}";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
