package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// GUID 3002 -- requestVmMigration
// Strategy: refuse to send a VM toward a destination that is provably a dead
// end -- already migrating, target host failed/dead/off, or target host
// lacking verified capacity. A migration into any of these conditions either
// errors, stalls, or has to be redone, all of which cost throughput.
public class executor_v2 implements Executor<int[]> {

    private static final int GUID = 3002;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Malformed payload for requestVmMigration, expected 2 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] No prescribed action this cycle.");
            return false;
        }

        int vmId = action[0];
        int targetHostId = action[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Unknown VM/host reference, aborting migration for VM " + vmId);
            return false;
        }
        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] VM " + vmId + " already migrating, skipping duplicate request.");
            return false;
        }
        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost) || actionSpace.isHostPoweredDown(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Target host " + targetHostId + " is failed/dead/off, skipping migration to avoid stranding VM " + vmId);
            return false;
        }
        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Target host " + targetHostId + " lacks capacity for VM " + vmId + ", skipping migration.");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Requested migration of VM " + vmId + " to host " + targetHostId);
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
        return "Migrate a VM to a target host only once its capacity and liveness have been verified, avoiding dead-end destinations";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
