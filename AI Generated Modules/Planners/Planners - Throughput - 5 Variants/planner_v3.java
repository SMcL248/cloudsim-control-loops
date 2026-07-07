package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

/**
 * planner_v3 - VM Total Remaining Length Balancer
 *
 * Metric: total remaining cloudlet length across a VM's exec list
 * (sum of getRemainingCloudletLength() over all queued cloudlets).
 *
 * Strategy:
 *   1. Among OVERLOADED VMs with a non-empty exec list, select the one
 *      carrying the highest total remaining length as the migration
 *      source.
 *   2. From the source VM, migrate its single heaviest cloudlet (largest
 *      remaining length), which offloads the most work in one move.
 *   3. Among UNDERLOADED VMs (falling back to BALANCED if none exist),
 *      excluding the source, select the destination with the lowest
 *      total remaining length.
 *   4. Feasibility check (length-based, not demand-based): only migrate
 *      if the destination's total remaining length after accepting the
 *      cloudlet would not exceed the source's original total.
 *
 * Input  GUID : vm-length-loadstate
 * Output GUID : cloudlet-migration
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final int[] NO_MIGRATION = new int[]{-1, -1, -1};

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vmList = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vmList.size()) {
            Log.printlnConcat(now, ": [planner_v3] Diagnosis length mismatch (expected ",
                    vmList.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return NO_MIGRATION;
        }

        // --- Step 1: select the source VM with the highest total remaining length ---
        GuestEntity sourceVm = null;
        double sourceLength = -1.0;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vmList.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
            if (execList.isEmpty()) continue;

            double totalLength = totalRemainingLength(execList);
            if (totalLength > sourceLength) {
                sourceLength = totalLength;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(now, ": [planner_v3] No overloaded VM with migratable cloudlets. No migration needed.");
            return NO_MIGRATION;
        }

        // --- Step 2: pick the heaviest cloudlet on the source VM ---
        List<Cloudlet> execList = sourceVm.getCloudletScheduler().getCloudletExecList();
        Cloudlet cloudletToMigrate = null;
        double maxLength = -1.0;

        for (Cloudlet c : execList) {
            double length = c.getRemainingCloudletLength();
            if (length > maxLength) {
                maxLength = length;
                cloudletToMigrate = c;
            }
        }

        if (cloudletToMigrate == null) {
            Log.printlnConcat(now, ": [planner_v3] Source VM ", sourceVm.getId(), " has no cloudlets to migrate.");
            return NO_MIGRATION;
        }

        // --- Step 3: find a destination VM by lowest total remaining length ---
        GuestEntity destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.UNDERLOADED);
        if (destVm == null) {
            destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.BALANCED);
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [planner_v3] No suitable destination VM found. Migration aborted.");
            return NO_MIGRATION;
        }

        // --- Step 4: length-based feasibility check ---
        double destLength = totalRemainingLength(destVm.getCloudletScheduler().getCloudletExecList());
        if (destLength + cloudletToMigrate.getRemainingCloudletLength() > sourceLength) {
            Log.printlnConcat(now, ": [planner_v3] Destination VM ", destVm.getId(),
                    " would not improve on source total length (", sourceLength, "). Migration aborted.");
            return NO_MIGRATION;
        }

        Log.printlnConcat(now, ": [planner_v3] Migration plan: Cloudlet ", cloudletToMigrate.getCloudletId(),
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

    /** Finds the VM with the lowest total remaining length matching the target LoadState, excluding the source. */
    private GuestEntity findDestination(List<GuestEntity> vmList, LoadState[] diagnosis,
                                         GuestEntity sourceVm, LoadState targetState) {
        GuestEntity best = null;
        double bestLength = Double.MAX_VALUE;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            GuestEntity vm = vmList.get(i);
            if (vm.getId() == sourceVm.getId()) continue;

            double length = totalRemainingLength(vm.getCloudletScheduler().getCloudletExecList());
            if (length < bestLength) {
                bestLength = length;
                best = vm;
            }
        }

        return best;
    }

    @Override
    public String inputGuid() {
        return "vm-length-loadstate";
    }

    @Override
    public String outputGuid() {
        return "cloudlet-migration";
    }
}
