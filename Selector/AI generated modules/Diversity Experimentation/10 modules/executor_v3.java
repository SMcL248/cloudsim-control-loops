package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Variant angle: requestVmCreation, guarded only by basic non-negativity sanity
// checks on the tier/datacenter indices (no ReadSpace call exposes the valid
// range for sizeTierIndex or datacenterId, so this variant is deliberately
// permissive beyond that and simply reports whether creation returned a VM).
public class executor_v3 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": [executor_v3] REJECTED malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        if (tierIndex < 0 || sizeTierIndex < 0 || datacenterId < 0) {
            Log.printlnConcat(now, ": [executor_v3] REJECTED creation, negative index/id in payload tierIndex=", tierIndex, " sizeTierIndex=", sizeTierIndex, " datacenterId=", datacenterId);
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        if (created == null) {
            Log.printlnConcat(now, ": [executor_v3] ATTEMPTED requestVmCreation tierIndex=", tierIndex, " sizeTierIndex=", sizeTierIndex, " datacenterId=", datacenterId, " result=null");
        } else {
            Log.printlnConcat(now, ": [executor_v3] ATTEMPTED requestVmCreation tierIndex=", tierIndex, " sizeTierIndex=", sizeTierIndex, " datacenterId=", datacenterId, " newVmId=", actionSpace.getId(created));
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "create new VM at tier";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
