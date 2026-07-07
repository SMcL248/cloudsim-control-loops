package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

/**
 * planner_v1 - VM Cloudlet-Count Balancer
 *
 * Metric: cloudlet count (size of a VM's exec list).
 *
 * Strategy:
 *   1. Among OVERLOADED VMs with a non-empty exec list, select the one
 *      carrying the highest cloudlet count as the migration source.
 *   2. From the source VM, migrate its cheapest-to-move cloudlet (the one
 *      with the smallest remaining length), minimising migration overhead
 *      while still reducing the source's count by one.
 *   3. Among UNDERLOADED VMs (falling back to BALANCED if none exist),
 *      excluding the source, select the destination with the lowest
 *      cloudlet count.
 *   4. Feasibility check (count-based, not demand-based): only migrate if
 *      the destination's count after accepting the cloudlet would still
 *      not exceed the source's original count.
 *
 * Input  GUID : vm-count-loadstate
 * Output GUID : cloudlet-migration
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    private static final int[] NO_MIGRATION = new int[]{-1, -1, -1};

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vmList = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vmList.size()) {
            Log.printlnConcat(now, ": [planner_v1] Diagnosis length mismatch (expected ",
                    vmList.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return NO_MIGRATION;
        }

        // --- Step 1: select the most overloaded source VM by cloudlet count ---
        GuestEntity sourceVm = null;
        int sourceCount = -1;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vmList.get(i);
            int count = vm.getCloudletScheduler().getCloudletExecList().size();
            if (count > 0 && count > sourceCount) {
                sourceCount = count;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(now, ": [planner_v1] No overloaded VM with migratable cloudlets. No migration needed.");
            return NO_MIGRATION;
        }

        // --- Step 2: pick the cheapest cloudlet on the source VM to migrate ---
        List<Cloudlet> execList = sourceVm.getCloudletScheduler().getCloudletExecList();
        Cloudlet cloudletToMigrate = null;
        double minLength = Double.MAX_VALUE;

        for (Cloudlet c : execList) {
            double length = c.getRemainingCloudletLength();
            if (length < minLength) {
                minLength = length;
                cloudletToMigrate = c;
            }
        }

        if (cloudletToMigrate == null) {
            Log.printlnConcat(now, ": [planner_v1] Source VM ", sourceVm.getId(), " has no cloudlets to migrate.");
            return NO_MIGRATION;
        }

        // --- Step 3: find a destination VM by lowest cloudlet count ---
        GuestEntity destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.UNDERLOADED);
        if (destVm == null) {
            destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.BALANCED);
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [planner_v1] No suitable destination VM found. Migration aborted.");
            return NO_MIGRATION;
        }

        // --- Step 4: count-based feasibility check ---
        int destCount = destVm.getCloudletScheduler().getCloudletExecList().size();
        if (destCount + 1 > sourceCount) {
            Log.printlnConcat(now, ": [planner_v1] Destination VM ", destVm.getId(),
                    " would not improve on source count (", sourceCount, "). Migration aborted.");
            return NO_MIGRATION;
        }

        Log.printlnConcat(now, ": [planner_v1] Migration plan: Cloudlet ", cloudletToMigrate.getCloudletId(),
                " from VM ", sourceVm.getId(), " -> VM ", destVm.getId());

        return new int[]{cloudletToMigrate.getCloudletId(), sourceVm.getId(), destVm.getId()};
    }

    /** Finds the VM with the lowest cloudlet count matching the target LoadState, excluding the source. */
    private GuestEntity findDestination(List<GuestEntity> vmList, LoadState[] diagnosis,
                                         GuestEntity sourceVm, LoadState targetState) {
        GuestEntity best = null;
        int bestCount = Integer.MAX_VALUE;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            GuestEntity vm = vmList.get(i);
            if (vm.getId() == sourceVm.getId()) continue;

            int count = vm.getCloudletScheduler().getCloudletExecList().size();
            if (count < bestCount) {
                bestCount = count;
                best = vm;
            }
        }

        return best;
    }

    @Override
    public String inputGuid() {
        return "vm-count-loadstate";
    }

    @Override
    public String outputGuid() {
        return "cloudlet-migration";
    }
}
