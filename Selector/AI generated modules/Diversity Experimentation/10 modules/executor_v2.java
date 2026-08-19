package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] rejected malformed payload, expected 2 ints");
            return false;
        }

        int vmId = actions[0];
        int targetHostId = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        HostEntity targetHost = actionSpace.getHostById(targetHostId);

        if (vm == null || targetHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] rejected, unresolved vmId=", vmId, " or hostId=", targetHostId);
            return false;
        }

        try {
            actionSpace.requestVmMigration(vm, targetHost);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] requestVmMigration threw, vmId=", vmId, " targetHostId=", targetHostId);
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] attempted requestVmMigration, vmId=", vmId, " targetHostId=", targetHostId);
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
