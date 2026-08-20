package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v6 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                    "malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = payload[0];
        int targetHostId = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                    "aborting migration, unresolved VM or host reference");
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                    "aborting migration, target host " + targetHostId + " is failed or permanently dead");
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                    "aborting migration, VM " + vmId + " is already mid-migration");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                    "aborting migration, target host " + targetHostId + " lacks sufficient resources for VM " + vmId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ",
                "capacity check passed, issued requestVmMigration vm=" + vmId + " target=" + targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Migrate a VM to a target host";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
