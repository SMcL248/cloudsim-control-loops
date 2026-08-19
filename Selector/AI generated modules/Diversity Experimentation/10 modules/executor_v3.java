package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] rejected malformed payload, expected 3 ints");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        GuestEntity created;
        try {
            created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        } catch (Exception e) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] requestVmCreation threw, tierIndex=", tierIndex, " sizeTierIndex=", sizeTierIndex, " datacenterId=", datacenterId);
            return false;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] attempted requestVmCreation, tierIndex=", tierIndex, " sizeTierIndex=", sizeTierIndex, " datacenterId=", datacenterId, " created=", (created == null ? "null" : String.valueOf(actionSpace.getId(created))));
        return true;
    }

    @Override
    public String inputSemantic() {
        return "create new VM at tier and datacenter";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
