package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v8 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ",
                    "malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = payload[0];
        int sizeTierIndex = payload[1];
        int datacenterId = payload[2];

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] ",
                "issued requestVmCreation tier=" + tierIndex + " sizeTier=" + sizeTierIndex + " dc=" + datacenterId
                        + " result=" + (created == null ? "null" : ("vm " + actionSpace.getId(created))));
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
