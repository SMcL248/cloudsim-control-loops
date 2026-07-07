package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

/**
 * planner_v5 - VM Average Cloudlet-Length Balancer
 *
 * Metric: "avglength" = total remaining cloudlet length divided by
 * cloudlet count (mean remaining length per cloudlet queued on the VM).
 * This flags VMs carrying a few unusually heavy cloudlets, which count-
 * or total-length-based views can miss or double-count.
 *
 * Strategy:
 *   1. Among OVERLOADED VMs with a non-empty exec list, select the one
 *      with the highest avglength as the migration source.
 *   2. From the source VM, migrate the cloudlet whose remaining length is
 *      closest to the source's avglength — a representative-sized
 *      cloudlet, avoiding skew from an atypical outlier.
 *   3. Among UNDERLOADED VMs (falling back to BALANCED if none exist),
 *      excluding the source, select the destination with the lowest
 *      avglength.
 *   4. Feasibility check (length/count-based, not demand-based): only
 *      migrate if the destination's avglength after accepting the
 *      cloudlet would not exceed the source's original avglength.
 *
 * Input  GUID : vm-avglength-loadstate
 * Output GUID : cloudlet-migration
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final int[] NO_MIGRATION = new int[]{-1, -1, -1};

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vmList = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vmList.size()) {
            Log.printlnConcat(now, ": [planner_v5] Diagnosis length mismatch (expected ",
                    vmList.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return NO_MIGRATION;
        }

        // --- Step 1: select the source VM with the highest average cloudlet length ---
        GuestEntity sourceVm = null;
        double sourceAvgLength = -1.0;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vmList.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
            if (execList.isEmpty()) continue;

            double avgLength = totalRemainingLength(execList) / execList.size();
            if (avgLength > sourceAvgLength) {
                sourceAvgLength = avgLength;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(now, ": [planner_v5] No overloaded VM with migratable cloudlets. No migration needed.");
            return NO_MIGRATION;
        }

        // --- Step 2: pick the cloudlet whose length is closest to the source's average ---
        List<Cloudlet> execList = sourceVm.getCloudletScheduler().getCloudletExecList();
        Cloudlet cloudletToMigrate = null;
        double bestDiff = Double.MAX_VALUE;

        for (Cloudlet c : execList) {
            double diff = Math.abs(c.getRemainingCloudletLength() - sourceAvgLength);
            if (diff < bestDiff) {
                bestDiff = diff;
                cloudletToMigrate = c;
            }
        }

        if (cloudletToMigrate == null) {
            Log.printlnConcat(now, ": [planner_v5] Source VM ", sourceVm.getId(), " has no cloudlets to migrate.");
            return NO_MIGRATION;
        }

        // --- Step 3: find a destination VM by lowest average cloudlet length ---
        GuestEntity destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.UNDERLOADED);
        if (destVm == null) {
            destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.BALANCED);
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [planner_v5] No suitable destination VM found. Migration aborted.");
            return NO_MIGRATION;
        }

        // --- Step 4: length/count-based feasibility check ---
        List<Cloudlet> destExecList = destVm.getCloudletScheduler().getCloudletExecList();
        double destTotalLength = totalRemainingLength(destExecList);
        int predictedCount = destExecList.size() + 1;
        double predictedAvgLength = (destTotalLength + cloudletToMigrate.getRemainingCloudletLength()) / predictedCount;

        if (predictedAvgLength > sourceAvgLength) {
            Log.printlnConcat(now, ": [planner_v5] Destination VM ", destVm.getId(),
                    " predicted avglength ", predictedAvgLength, " would not improve on source avglength ",
                    sourceAvgLength, ". Migration aborted.");
            return NO_MIGRATION;
        }

        Log.printlnConcat(now, ": [planner_v5] Migration plan: Cloudlet ", cloudletToMigrate.getCloudletId(),
                " from VM ", sourceVm.getId(), " -> VM ", destVm.getId());

        return new int[]{cloudletToMigrate.getCloudletId(), sourceVm.getId(), destVm.getId()};
    }

    /** Sum of remaining cloudlet length across an exec list. */
    private double totalRemainingLength(List<Cloudlet> execList) {
        double total = 0.0;
        for (Cloudlet c : execList) {
            total += c.getRemainingCloudletLength();
        }
        return total;
    }

    /** Finds the VM with the lowest average cloudlet length matching the target LoadState, excluding the source. */
    private GuestEntity findDestination(List<GuestEntity> vmList, LoadState[] diagnosis,
                                         GuestEntity sourceVm, LoadState targetState) {
        GuestEntity best = null;
        double bestAvgLength = Double.MAX_VALUE;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            GuestEntity vm = vmList.get(i);
            if (vm.getId() == sourceVm.getId()) continue;

            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
            double avgLength = execList.isEmpty() ? 0.0 : totalRemainingLength(execList) / execList.size();
            if (avgLength < bestAvgLength) {
                bestAvgLength = avgLength;
                best = vm;
            }
        }

        return best;
    }

    @Override
    public String inputGuid() {
        return "vm-avglength-loadstate";
    }

    @Override
    public String outputGuid() {
        return "cloudlet-migration";
    }
}
