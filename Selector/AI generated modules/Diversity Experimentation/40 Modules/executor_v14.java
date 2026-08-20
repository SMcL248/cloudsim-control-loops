package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v14 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ",
                    "aborting destruction, VM " + vmId + " does not resolve to a known entity");
            return false;
        }

        if (actionSpace.isVmBeingInstantiated(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ",
                    "VM " + vmId + " is still being instantiated, destruction request issued regardless");
        }

        actionSpace.requestVmDestruction(vm);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v14] ",
                "existence check passed, issued requestVmDestruction vm=" + vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Destroy a VM";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
