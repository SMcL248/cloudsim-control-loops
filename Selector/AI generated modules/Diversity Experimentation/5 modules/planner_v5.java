package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * planner_v5
 *
 * Strategy: "Cloudlet-level load balancing"
 * Cloudlet-level diagnosis. Among cloudlets flagged OVERLOADED, selects the
 * one with the largest remaining length (the worst backlog), locates its
 * current host VM by searching every VM's cloudlet list, then relocates it
 * to whichever other VM in the system currently has the lowest CPU
 * utilisation.
 * Emits moveCloudlet{cloudletId, fromVmId, toVmId}, or an empty array if no
 * source/destination pair can be established.
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int limit = Math.min(diagnosis.length, cloudlets.size());

        Cloudlet worstCloudlet = null;
        long worstRemaining = -1L;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            Cloudlet cl = cloudlets.get(i);
            long remaining = readSpace.getRemainingLength(cl);
            if (remaining > worstRemaining) {
                worstRemaining = remaining;
                worstCloudlet = cl;
            }
        }

        if (worstCloudlet == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "no overloaded cloudlet found");
            return new int[0];
        }

        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity sourceVm = findOwningVm(readSpace, vms, worstCloudlet);
        if (sourceVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ",
                    "could not locate owning vm for overloaded cloudlet");
            return new int[0];
        }

        GuestEntity destinationVm = findLeastLoadedOtherVm(readSpace, vms, sourceVm);
        if (destinationVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "no destination vm available for cloudlet move");
            return new int[0];
        }

        int cloudletId = readSpace.getId(worstCloudlet);
        int fromVmId = readSpace.getId(sourceVm);
        int toVmId = readSpace.getId(destinationVm);

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ",
                "moving cloudlet " + cloudletId + " from vm " + fromVmId + " to vm " + toVmId);
        return new int[] { cloudletId, fromVmId, toVmId };
    }

    private GuestEntity findOwningVm(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet cl) {
        int targetId = readSpace.getId(cl);
        for (GuestEntity vm : vms) {
            for (Cloudlet candidate : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(candidate) == targetId) {
                    return vm;
                }
            }
        }
        return null;
    }

    private GuestEntity findLeastLoadedOtherVm(ReadSpace readSpace, List<GuestEntity> vms, GuestEntity exclude) {
        GuestEntity best = null;
        double lowestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : vms) {
            if (vm == exclude) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double util = readSpace.getVmCpuUtil(vm);
            if (util < lowestUtil) {
                lowestUtil = util;
                best = vm;
            }
        }
        return best;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-classification";
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
