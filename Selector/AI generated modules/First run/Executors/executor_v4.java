package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: requestVmCreation -- throughput. Provisions a new vm at the given mips/size tier
// to absorb incoming workload.
public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] invalid payload, aborting requestVmCreation");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        if (tierIndex == -1){
            return false;
        }

        GuestEntity vm = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] requestVmCreation attempted but returned no vm, tier=", tierIndex, ", sizeTier=", sizeTierIndex, ", datacenter=", datacenterId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] created vm id=", actionSpace.getId(vm), " at tier=", tierIndex, ", sizeTier=", sizeTierIndex, ", datacenter=", datacenterId);
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }
}
