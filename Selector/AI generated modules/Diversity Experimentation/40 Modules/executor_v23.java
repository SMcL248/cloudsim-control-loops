package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v23 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v23] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v23] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        double newBw;
        try {
            newBw = actionSpace.getBwTiers()[tierIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v23] ",
                    "tierIndex " + tierIndex + " out of bounds, scaling not attempted");
            return false;
        }

        boolean succeeded = actionSpace.requestBwScaling(vm, newBw);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v23] ",
                "issued requestBwScaling vm=" + vmId + " tier=" + tierIndex + " succeeded=" + succeeded);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale VM bandwidth to a tier";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
