package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class executor_v28 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v28] ",
                    "malformed payload, expected {vmId}");
            return false;
        }

        int vmId = payload[0];
        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v28] ",
                    "aborting allocation, unresolved VM reference for id " + vmId);
            return false;
        }

        HostEntity host = findHostOf(vm, actionSpace);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v28] ",
                    "aborting allocation, VM " + vmId + " is not currently hosted anywhere");
            return false;
        }

        if (actionSpace.isHostFailed(host) || !actionSpace.hostHasFreePe(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v28] ",
                    "aborting allocation, host has no free PE headroom or is failed for VM " + vmId);
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v28] ",
                "host headroom check passed, issued requestPeAllocation vm=" + vmId + " succeeded=" + succeeded);
        return true;
    }

    private HostEntity findHostOf(GuestEntity vm, ActionSpace actionSpace) {
        List<HostEntity> hosts = actionSpace.getAllHosts();
        if (hosts == null) {
            return null;
        }
        for (HostEntity host : hosts) {
            List<GuestEntity> hosted = actionSpace.getVmListForHost(host);
            if (hosted != null && hosted.contains(vm)) {
                return host;
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "Allocate a PE to a VM";
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
