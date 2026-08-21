package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: capacity- and health-gated migration.
// Refuses to fire the migration request unless the target host is
// confirmed live (not failed, not permanently dead) and
// canMigrateGuestToHost confirms it actually has room, rather than
// trusting the planner's target choice and letting the platform reject
// it silently.
public class executor_v2 implements Executor<int[]> {

    private static final int INPUT_GUID = 3002;
    private static final String INPUT_SEMANTIC = "migrate a VM to a target host";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 2) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] no-op or malformed payload received, skipping requestVmMigration");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] VM ", vmId, " or target host ", targetHostId, " not resolvable, skipping migration");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] target host ", targetHostId, " is failed or permanently dead, refusing migration");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] target host ", targetHostId, " lacks capacity for VM ", vmId, ", refusing migration");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] requested migration of VM ", vmId, " to host ", targetHostId);
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
