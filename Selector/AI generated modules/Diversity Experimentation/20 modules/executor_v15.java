package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Eligibility-Gated Executor for requestPeAllocation (3008).
// Locates the VM's current host and checks it is healthy and has PE
// headroom before requesting an additional PE for the VM.
public class executor_v15 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v15] ", "Malformed payload, expected {vmId}");
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v15] ", "Rejected allocation, unknown vm=" + vmId);
            return false;
        }

        HostEntity currentHost = null;
        List<HostEntity> hosts = actionSpace.getAllHosts();
        for (HostEntity host : hosts) {
            if (actionSpace.getVmListForHost(host).contains(vm)) {
                currentHost = host;
                break;
            }
        }

        if (currentHost == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v15] ", "Rejected allocation, vm=" + vmId + " is not yet resident on any host");
            return false;
        }

        if (actionSpace.isHostFailed(currentHost) || !actionSpace.hostHasFreePe(currentHost)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v15] ", "Rejected allocation, host=" + actionSpace.getId(currentHost) + " is failed or has no free pe headroom");
            return false;
        }

        boolean ok = actionSpace.requestPeAllocation(vm);
        if (ok) {
            successfulActionCount++;
        }
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v15] ", "Requested pe allocation for vm=" + vmId + " success=" + ok);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestPeAllocation: allocate an additional PE to a VM";
    }

    @Override
    public int inputGuid() {
        return 3008;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
