package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: requestPeAllocation -- throughput. Grants a vm an extra processing element
// so it can absorb more concurrent cloudlet work.
public class executor_v7 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] invalid payload, aborting requestPeAllocation");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] vm not found, id=", vmId, ", aborting");
            return false;
        }

        boolean success = actionSpace.requestPeAllocation(vm);

        if (success) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] requested pe allocation for vm=", vmId, ", success=", success);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeAllocation";
    }

    @Override
    public int inputGuid() {
        return 3012;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
