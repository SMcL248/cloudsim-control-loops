package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class executor_v20 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ",
                    "aborting scaling, unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getRamTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ",
                    "aborting scaling, tierIndex " + tierIndex + " out of range [0," + tiers.length + ")");
            return false;
        }

        double requestedRam = tiers[tierIndex];
        double delta = requestedRam - actionSpace.getVmRam(vm);

        HostEntity host = findHostOf(vm, actionSpace);
        if (host != null && delta > 0 && actionSpace.getHostAvailableRam(host) < delta) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ",
                    "aborting scaling, host has insufficient RAM headroom for VM " + vmId);
            return false;
        }

        boolean succeeded = actionSpace.requestRamScaling(vm, requestedRam);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v20] ",
                "headroom check passed, issued requestRamScaling vm=" + vmId + " succeeded=" + succeeded);
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
        return "Scale VM RAM to a tier";
    }

    @Override
    public int inputGuid() {
        return 3006;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
