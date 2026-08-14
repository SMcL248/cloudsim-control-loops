package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestVmCreation action: provisions a new VM of the given tier/size in the given datacenter.
public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] invalid payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] requested vm creation, tier ", tierIndex, " size ", sizeTierIndex, " datacenter ", datacenterId, " -- no vm returned");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] created vm ", actionSpace.getId(created), " tier ", tierIndex, " size ", sizeTierIndex, " in datacenter ", datacenterId);
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation(tierIndex, sizeTierIndex, datacenterId)";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
