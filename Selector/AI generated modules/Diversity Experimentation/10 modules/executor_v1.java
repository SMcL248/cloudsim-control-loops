package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

public class executor_v1 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] rejected malformed payload, expected 3 ints");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        try {
            actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] moveCloudlet threw, cloudletId=", cloudletId, " fromVmId=", fromVmId, " toVmId=", toVmId);
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] attempted moveCloudlet, cloudletId=", cloudletId, " fromVmId=", fromVmId, " toVmId=", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "move cloudlet between VMs";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }
}
