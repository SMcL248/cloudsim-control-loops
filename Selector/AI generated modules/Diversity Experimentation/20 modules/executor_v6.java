package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Outcome-Verifying Pass-through Executor for requestVmCreation (3003).
// Trusts the planner's tier indices completely and dispatches them
// unmodified; differentiates by producing a detailed post-creation
// confirmation log (or a clear failure diagnosis) rather than pre-validating.
public class executor_v6 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ", "Malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ", "Creation rejected for tierIndex=" + tierIndex + " sizeTierIndex=" + sizeTierIndex + " datacenter=" + datacenterId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] ", "Created vm=" + actionSpace.getId(created) + " mips=" + actionSpace.getVmMips(created) + " ram=" + actionSpace.getVmRam(created) + " bw=" + actionSpace.getVmBw(created));
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation: create a new VM at a given tier and size";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
