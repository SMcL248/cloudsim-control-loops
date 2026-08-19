package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] rejected malformed payload, expected 1 int");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] rejected, unresolved vmId=", vmId);
            return false;
        }

        try {
            actionSpace.requestVmDestruction(vm);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] requestVmDestruction threw, vmId=", vmId);
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] attempted requestVmDestruction, vmId=", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "destroy VM";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
