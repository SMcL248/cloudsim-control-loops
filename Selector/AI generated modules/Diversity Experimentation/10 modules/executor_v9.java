package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v9 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] rejected malformed payload, expected 1 int");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] rejected, unresolved vmId=", vmId);
            return false;
        }

        boolean succeeded;
        try {
            succeeded = actionSpace.requestPeDeallocation(vm);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] requestPeDeallocation threw, vmId=", vmId);
            return false;
        }

        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] attempted requestPeDeallocation, vmId=", vmId, " succeeded=", succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "deallocate PE from VM";
    }

    @Override
    public int inputGuid() {
        return 3009;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
