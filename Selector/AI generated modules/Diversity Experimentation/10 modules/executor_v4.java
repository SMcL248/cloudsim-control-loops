package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

// Variant angle: requestVmDestruction, guarded by resolving the VM first and
// logging the blast radius (how many in-flight cloudlets will be destroyed
// alongside it) before firing, since this is an explicitly destructive action.
public class executor_v4 implements Executor<int[]> {

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v4] REJECTED malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v4] REJECTED destruction, VM ", vmId, " could not be resolved");
            return false;
        }

        List<Cloudlet> lostCloudlets = actionSpace.getVmCloudletList(vm);
        int lostCount = (lostCloudlets == null) ? 0 : lostCloudlets.size();

        Log.printlnConcat(now, ": [executor_v4] WARNING destroying VM ", vmId, " will destroy ", lostCount, " allocated cloudlet workload(s)");

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(now, ": [executor_v4] ATTEMPTED requestVmDestruction vm=", vmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "destroy VM";
    }

    @Override
    public int inputGuid() {
        return 3004;
    }
}
