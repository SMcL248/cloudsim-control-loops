package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v16 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();
        int limit = Math.min(diagnosis.length, activeCloudlets.size());

        Cloudlet worstCloudlet = null;
        GuestEntity worstFromVm = null;
        double worstFinishTime = -1;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cloudlet = activeCloudlets.get(i);
            GuestEntity fromVm = findOwningVm(cloudlet, readSpace);
            if (fromVm == null) {
                continue;
            }
            double estimatedFinish = readSpace.getCloudletEstimatedFinishTime(fromVm, cloudlet);
            if (estimatedFinish > worstFinishTime) {
                worstFinishTime = estimatedFinish;
                worstCloudlet = cloudlet;
                worstFromVm = fromVm;
            }
        }

        if (worstCloudlet == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v16] no overloaded cloudlet with resolvable owner VM found");
            return new int[0];
        }

        GuestEntity bestTargetVm = null;
        double bestFinishTime = Double.MAX_VALUE;
        for (GuestEntity vm : readSpace.getVmList()) {
            if (vm == worstFromVm || readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double projectedFinish = readSpace.getCloudletEstimatedFinishTime(vm, worstCloudlet);
            if (projectedFinish < bestFinishTime) {
                bestFinishTime = projectedFinish;
                bestTargetVm = vm;
            }
        }

        if (bestTargetVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v16] no candidate target VM found for at-risk cloudlet");
            return new int[0];
        }

        int cloudletId = readSpace.getId(worstCloudlet);
        int fromVmId = readSpace.getId(worstFromVm);
        int toVmId = readSpace.getId(bestTargetVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v16] moving at-risk cloudlet ", cloudletId, " (est. finish=", worstFinishTime, ") from VM ", fromVmId, " to VM ", toVmId, " (projected finish=", bestFinishTime, ")");
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
        return "cloudlet-loadstate-progress-risk";
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
