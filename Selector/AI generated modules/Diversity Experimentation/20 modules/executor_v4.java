package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Idempotency and Motion-State Guard for requestVmMigration (3002).
// Instead of checking destination capacity, this variant checks whether the
// VM is already in motion or already resident on the target host, avoiding
// duplicate or pointless migration requests.
public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ", "Malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);
        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ", "Rejected migration, unknown reference vm=" + vmId + " host=" + targetHostId);
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ", "Skipped migration, vm=" + vmId + " already has a migration in flight");
            return false;
        }

        if (actionSpace.getVmListForHost(targetHost).contains(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ", "Skipped migration, vm=" + vmId + " already resides on host=" + targetHostId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] ", "Requested migration of vm=" + vmId + " to host=" + targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmMigration: relocate a VM to a target host";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
