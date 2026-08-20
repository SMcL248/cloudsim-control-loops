package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ",
                    "malformed payload, expected {cloudletId, fromVmId, toVmId}");
            return false;
        }

        int cloudletId = payload[0];
        int fromVmId = payload[1];
        int toVmId = payload[2];

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] ",
                "issued moveCloudlet cloudlet=" + cloudletId + " from=" + fromVmId + " to=" + toVmId
                        + " with no precondition checks");
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Move a cloudlet from one VM to another";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
