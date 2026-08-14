package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Executes the requestVmMigration action: migrates a VM onto a specified target host.
public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] invalid payload, expected {vmId, targetHostId}");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] cannot resolve vm ", vmId, " or host ", targetHostId, ", aborting");
            return false;
        }

        actionSpace.requestVmMigration(vm, targetHost);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] requested migration of vm ", vmId, " to host ", targetHostId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmMigration(vmId, targetHostId)";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
