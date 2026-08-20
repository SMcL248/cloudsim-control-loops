package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class executor_v24 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v24] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v24] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getBwTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v24] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range [0," + tiers.length + ")");
            return false;
        }

        double requestedBw = tiers[tierIndex];
        double delta = requestedBw - actionSpace.getVmBw(vm);

        HostEntity host = findHostOf(vm, actionSpace);
        if (host != null && delta > 0 && actionSpace.getHostAvailableBw(host) < delta) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v24] ",
                    "aborting scaling, host has insufficient bandwidth headroom for VM " + vmId);
            return false;
        }

        boolean succeeded = actionSpace.requestBwScaling(vm, requestedBw);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v24] ",
                "headroom check passed, issued requestBwScaling vm=" + vmId + " succeeded=" + succeeded);
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
        return "Scale VM bandwidth to a tier";
    }

    @Override
    public int inputGuid() {
        return 3007;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
