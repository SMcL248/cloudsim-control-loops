package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v2 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Malformed payload for requestVmCreation, expected 3 ints, action not attempted.");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] Requesting VM creation with mips/ram/bw tier ", tierIndex, ", size tier ", sizeTierIndex, ", in datacenter ", datacenterId, ".");
        GuestEntity vm = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] VM creation request returned null, tier/size/datacenter combination was invalid.");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] VM creation request accepted, new VM id ", actionSpace.getId(vm), ".");
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
