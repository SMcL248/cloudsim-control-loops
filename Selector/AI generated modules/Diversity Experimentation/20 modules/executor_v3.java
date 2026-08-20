package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Eligibility-Gated Executor for requestVmMigration (3002).
// Confirms the target host is not failed/dead and has sufficient headroom
// for the VM (canMigrateGuestToHost) before dispatching the request.
public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ", "Malformed payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);
        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ", "Rejected migration, unknown reference vm=" + vmId + " host=" + targetHostId);
            return false;
        }

        if (actionSpace.isHostFailed(targetHost) || actionSpace.isHostPermanentlyDead(targetHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ", "Rejected migration, target host=" + targetHostId + " is failed or permanently dead");
            return false;
        }

        if (!actionSpace.canMigrateGuestToHost(targetHost, vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ", "Rejected migration, target host=" + targetHostId + " lacks capacity for vm=" + vmId);
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] ", "Requested migration of vm=" + vmId + " to host=" + targetHostId);
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
