package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v11 implements Executor<int[]> {

    private static final int MAX_ATTEMPTS = 3;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ",
                    "malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = payload[0];
        int sizeTierIndex = payload[1];
        int datacenterId = payload[2];

        GuestEntity created = null;
        int attempt = 0;

        while (attempt < MAX_ATTEMPTS && created == null) {
            attempt++;
            created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

            if (created == null) {
                Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ",
                        "requestVmCreation attempt " + attempt + " of " + MAX_ATTEMPTS + " was rejected");
            }
        }

        if (created != null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ",
                    "requestVmCreation succeeded on attempt " + attempt);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v11] ",
                    "requestVmCreation exhausted " + MAX_ATTEMPTS + " attempts without success");
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
