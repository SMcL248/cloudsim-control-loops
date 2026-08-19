package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Malformed payload for requestVmMigration, expected 2 ints, action not attempted.");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] VM ", vmId, " could not be resolved, migration not attempted.");
            return false;
        }
        if (targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Host ", targetHostId, " could not be resolved, migration not attempted.");
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] Requesting migration of VM ", vmId, " to host ", targetHostId, ".");
        actionSpace.requestVmMigration(vm, targetHost);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 3002;
    }
}
