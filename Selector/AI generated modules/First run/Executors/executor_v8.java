package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: requestPeDeallocation -- power. Reclaims a spare processing element from a vm,
// but refuses to strip a vm down to zero pes.
public class executor_v8 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] invalid payload, aborting requestPeDeallocation");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] vm not found, id=", vmId, ", aborting");
            return false;
        }

        if (actionSpace.getVmNumberOfPes(vm) <= 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] vm=", vmId, " has only one pe left, refusing to strand it");
            return false;
        }

        boolean success = actionSpace.requestPeDeallocation(vm);

        if (success) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v8] requested pe deallocation for vm=", vmId, ", success=", success);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeDeallocation";
    }

    @Override
    public int inputGuid() {
        return 3013;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
