package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v20 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();
        int limit = Math.min(diagnosis.length, activeCloudlets.size());

        Cloudlet stuckCloudlet = null;
        GuestEntity stuckFromVm = null;
        double worstRemainingRatio = -1;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cloudlet = activeCloudlets.get(i);
            long total = readSpace.getTotalLength(cloudlet);
            if (total <= 0) {
                continue;
            }
            double remainingRatio = (double) readSpace.getRemainingLength(cloudlet) / (double) total;
            if (remainingRatio > worstRemainingRatio) {
                GuestEntity fromVm = findOwningVm(cloudlet, readSpace);
                if (fromVm == null) {
                    continue;
                }
                worstRemainingRatio = remainingRatio;
                stuckCloudlet = cloudlet;
                stuckFromVm = fromVm;
            }
        }

        if (stuckCloudlet == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v20] no stalled overloaded cloudlet with resolvable owner found");
            return new int[0];
        }

        GuestEntity fastestVm = null;
        double highestThroughput = -1;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (vm == stuckFromVm || readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double throughput = readSpace.getVmEffectiveThroughput(vm);
            if (throughput > highestThroughput) {
                highestThroughput = throughput;
                fastestVm = vm;
            }
        }

        if (fastestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v20] no eligible high-throughput target VM found");
            return new int[0];
        }

        int cloudletId = readSpace.getId(stuckCloudlet);
        int fromVmId = readSpace.getId(stuckFromVm);
        int toVmId = readSpace.getId(fastestVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v20] moving stalled cloudlet ", cloudletId, " (remaining ratio=", worstRemainingRatio, ") from VM ", fromVmId, " to highest-throughput VM ", toVmId, " (throughput=", highestThroughput, ")");
        return new int[] { cloudletId, fromVmId, toVmId };
    }

    private GuestEntity findOwningVm(Cloudlet cloudlet, ReadSpace readSpace) {
        int cloudletId = readSpace.getId(cloudlet);
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(cl) == cloudletId) {
                    return vm;
                }
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-completion-risk";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-move";
    }

    @Override
    public int inputGuid() {
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }
}
