package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v10 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ",
                    "malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = payload[0];
        int sizeTierIndex = payload[1];
        int datacenterId = payload[2];

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ",
                    "requestVmCreation was rejected by the platform for tier=" + tierIndex
                            + " sizeTier=" + sizeTierIndex + " dc=" + datacenterId);
        } else if (actionSpace.isVmBeingInstantiated(created)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ",
                    "requestVmCreation accepted, VM pending allocation to a host");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v10] ",
                    "requestVmCreation accepted, VM " + actionSpace.getId(created) + " already allocated");
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "Create a new VM";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
