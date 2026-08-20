package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v12 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ",
                    "unresolved VM reference for id " + vmId + ", cannot issue destruction");
            return false;
        }

        actionSpace.requestVmDestruction(vm);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v12] ",
                "issued requestVmDestruction vm=" + vmId + " with no further checks");
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
