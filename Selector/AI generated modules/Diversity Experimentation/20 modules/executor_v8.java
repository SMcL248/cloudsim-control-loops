package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Existence and Lifecycle Guard for requestVmDestruction (3004).
// Refuses to destroy a VM that has not yet been placed on a host, since its
// host-side bookkeeping may not exist and the outcome would be ambiguous.
public class executor_v8 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ", "Nothing to destroy, unknown vm=" + vmId);
            return false;
        }

        if (actionSpace.isVmBeingInstantiated(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ", "Refused destruction, vm=" + vmId + " is still being instantiated and not yet host-resident");
            return false;
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ", "Destroyed vm=" + vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmDestruction: destroy a VM and its workload";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
