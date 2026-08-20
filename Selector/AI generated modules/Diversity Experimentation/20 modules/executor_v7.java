package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

// Impact-Assessment / Audit-first Executor for requestVmDestruction (3004).
// Before destroying, tallies the workload that will be lost and writes it
// to the log for traceability, then proceeds with the destruction as
// instructed since the decision itself was already made upstream.
public class executor_v7 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ", "Nothing to destroy, unknown vm=" + vmId);
            return false;
        }

        List<Cloudlet> workload = actionSpace.getVmCloudletList(vm);
        long remainingWork = 0L;
        for (Cloudlet cl : workload) {
            remainingWork += actionSpace.getRemainingLength(cl);
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ", "Destroying vm=" + vmId + " will drop " + workload.size() + " cloudlets, remainingWork=" + remainingWork + " MI");

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v7] ", "Destroyed vm=" + vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmDestruction: destroy a VM and its workload";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
