package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: requestVmDestruction -- power. Reclaims an idle vm's resources, but defers
// destruction if the vm is mid-migration to avoid destroying a vm in an inconsistent state.
public class executor_v5 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] invalid payload, aborting requestVmDestruction");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] vm not found, id=", vmId, ", aborting");
            return false;
        }

        if (actionSpace.isVmMigrating(vm)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] vm=", vmId, " is mid-migration, deferring destruction");
            return false;
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v5] requested destruction of vm=", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmDestruction";
    }

    @Override
    public int inputGuid() {
        return 3008;
    }
}
