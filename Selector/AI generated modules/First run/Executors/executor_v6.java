package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: requestMipsScaling -- throughput. Scales a vm's mips to the requested tier,
// resolving the tier index against getMipsTiers() and rejecting out-of-range indices.
public class executor_v6 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] invalid payload, aborting requestMipsScaling");
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] vm not found, id=", vmId, ", aborting");
            return false;
        }

        int[] mipsTiers = actionSpace.getMipsTiers();
        if (mipsTiers == null || tierIndex < 0 || tierIndex >= mipsTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] mips tier index out of range, tierIndex=", tierIndex, ", aborting");
            return false;
        }

        double newMips = mipsTiers[tierIndex];
        boolean success = actionSpace.requestMipsScaling(vm, newMips);

        if (success) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v6] requested mips scaling for vm=", vmId, " to tier=", tierIndex, ", success=", success);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestMipsScaling";
    }

    @Override
    public int inputGuid() {
        return 3009;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
