package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v10 - Cloudlet hotspot relief planner (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-cloudlet LoadState[] (index i corresponds to
 *   readSpace.getActiveCloudlets().get(i)), reflecting the load state of
 *   whichever VM currently runs each cloudlet. Finds the first cloudlet
 *   flagged OVERLOADED, locates its current VM, and moves the cloudlet to
 *   whichever other VM has the lowest CPU utilisation, relieving the
 *   hotspot without waiting for a full VM migration.
 *
 * Input semantic  : cloudlet-loadstate-hotspot (GUID 2400)
 * Output semantic : cloudlet-move              (GUID 3003, moveCloudlet)
 */
public class planner_v10 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<Cloudlet> activeCloudlets = readSpace.getActiveCloudlets();

        if (diagnosis == null || diagnosis.length != activeCloudlets.size()) {
            Log.printlnConcat(now, ": [planner_v10] Diagnosis/cloudlet size mismatch. No-op.");
            return new int[]{-1, -1, -1};
        }

        List<GuestEntity> vms = readSpace.getVmList();

        for (int i = 0; i < activeCloudlets.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            Cloudlet cloudlet = activeCloudlets.get(i);
            GuestEntity sourceVm = findOwningVm(cloudlet, vms, readSpace);
            if (sourceVm == null) continue;

            GuestEntity destVm = findLeastLoadedOther(sourceVm, vms, readSpace);
            if (destVm == null) continue;

            int cloudletId = readSpace.getId(cloudlet);
            int fromVmId = readSpace.getId(sourceVm);
            int toVmId = readSpace.getId(destVm);
            Log.printlnConcat(now, ": [planner_v10] Plan move cloudlet ", cloudletId,
                    " from hot VM ", fromVmId, " -> cooler VM ", toVmId);
            return new int[]{cloudletId, fromVmId, toVmId};
        }

        Log.printlnConcat(now, ": [planner_v10] No overloaded cloudlet needing relief. No-op.");
        return new int[]{-1, -1, -1};
    }

    private GuestEntity findOwningVm(Cloudlet cloudlet, List<GuestEntity> vms, ReadSpace readSpace) {
        int cloudletId = readSpace.getId(cloudlet);
        for (GuestEntity vm : vms) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(cl) == cloudletId) return vm;
            }
        }
        return null;
    }

    private GuestEntity findLeastLoadedOther(GuestEntity source, List<GuestEntity> vms, ReadSpace readSpace) {
        int sourceId = readSpace.getId(source);
        GuestEntity best = null;
        double bestUtil = Double.MAX_VALUE;

        for (GuestEntity vm : vms) {
            if (readSpace.getId(vm) == sourceId) continue;
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            double util = readSpace.getVmCpuUtil(vm);
            if (util < bestUtil) {
                bestUtil = util;
                best = vm;
            }
        }
        return best;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-hotspot";
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
        return 3003;
    }
}
