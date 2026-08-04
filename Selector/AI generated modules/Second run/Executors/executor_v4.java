package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executes the requestVmDestruction action: permanently destroys a VM and its hosted Cloudlet workloads.
public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] invalid payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] cannot resolve vm ", vmId, ", aborting");
            return false;
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v4] requested destruction of vm ", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmDestruction(vmId)";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
