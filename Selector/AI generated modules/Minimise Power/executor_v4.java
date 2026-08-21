package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestVmDestruction - GUID suffix 04.
public class executor_v4 implements Executor<int[]> {

    private static final int GUID = 3004;
    private static final String SEMANTIC =
        "requestVmDestruction: permanently tear down a VM and any Cloudlet workload allocated to it. Payload {vmId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(now, ": [executor_v4] rejected - payload must have exactly 1 entry {vmId}.");
            return false;
        }

        if (actions[0] == -1) {
            Log.printlnConcat(now, ": [executor_v4] no-op sentinel received, nothing to destroy.");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(now, ": [executor_v4] rejected - VM ", vmId, " could not be resolved.");
            return false;
        }

        int strandedCloudlets = actionSpace.getVmCloudletList(vm).size();
        if (strandedCloudlets > 0) {
            Log.printlnConcat(now, ": [executor_v4] warning - destroying VM ", vmId, " will discard ", strandedCloudlets, " attached cloudlet(s).");
        }

        actionSpace.requestVmDestruction(vm);
        Log.printlnConcat(now, ": [executor_v4] requested destruction of VM ", vmId, ".");
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}
