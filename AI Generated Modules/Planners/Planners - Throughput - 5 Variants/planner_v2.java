package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

/**
 * planner_v2 - VM Estimated-Time-to-Completion (ETC) Balancer
 *
 * Metric: max estimated finish-time duration across a VM's exec list,
 * i.e. max(getEstimatedFinishTime(cloudlet, now) - now).
 *
 * Strategy:
 *   1. Among OVERLOADED VMs with a non-empty exec list, select the one
 *      with the highest max-ETC as the migration source (the deepest
 *      time-backlog).
 *   2. From the source VM, migrate the single cloudlet with the largest
 *      individual ETC — the "long pole" driving the backlog.
 *   3. Among UNDERLOADED VMs (falling back to BALANCED if none exist),
 *      excluding the source, select the destination with the lowest
 *      max-ETC.
 *   4. Feasibility check (length/etc-based, not demand-based): the
 *      migrating cloudlet's completion time on the destination is
 *      approximated as remainingLength / destination.getMips(). Only
 *      migrate if max(destination max-ETC, this estimate) does not
 *      exceed the source's original max-ETC.
 *
 * Input  GUID : vm-etc-loadstate
 * Output GUID : cloudlet-migration
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final int[] NO_MIGRATION = new int[]{-1, -1, -1};

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vmList = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vmList.size()) {
            Log.printlnConcat(now, ": [planner_v2] Diagnosis length mismatch (expected ",
                    vmList.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return NO_MIGRATION;
        }

        // --- Step 1: select the source VM with the highest max-ETC ---
        GuestEntity sourceVm = null;
        double sourceEtc = -1.0;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vmList.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
            if (execList.isEmpty()) continue;

            double maxEtc = maxEtc(vm, execList, now);
            if (maxEtc > sourceEtc) {
                sourceEtc = maxEtc;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(now, ": [planner_v2] No overloaded VM with migratable cloudlets. No migration needed.");
            return NO_MIGRATION;
        }

        // --- Step 2: pick the cloudlet with the largest individual ETC ---
        List<Cloudlet> execList = sourceVm.getCloudletScheduler().getCloudletExecList();
        Cloudlet cloudletToMigrate = null;
        double bestEtc = -1.0;

        for (Cloudlet c : execList) {
            double etc = sourceVm.getCloudletScheduler().getEstimatedFinishTime(c, now) - now;
            if (etc > bestEtc) {
                bestEtc = etc;
                cloudletToMigrate = c;
            }
        }

        if (cloudletToMigrate == null) {
            Log.printlnConcat(now, ": [planner_v2] Source VM ", sourceVm.getId(), " has no cloudlets to migrate.");
            return NO_MIGRATION;
        }

        // --- Step 3: find a destination VM by lowest max-ETC ---
        GuestEntity destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.UNDERLOADED, now);
        if (destVm == null) {
            destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.BALANCED, now);
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [planner_v2] No suitable destination VM found. Migration aborted.");
            return NO_MIGRATION;
        }

        // --- Step 4: length/etc-based feasibility check ---
        double destMips = destVm.getMips();
        double destEtc = maxEtc(destVm, destVm.getCloudletScheduler().getCloudletExecList(), now);
        double estimatedNewEtc = (destMips > 0.0) ? cloudletToMigrate.getRemainingCloudletLength() / destMips : Double.MAX_VALUE;
        double predictedMaxEtc = Math.max(destEtc, estimatedNewEtc);

        if (predictedMaxEtc > sourceEtc) {
            Log.printlnConcat(now, ": [planner_v2] Destination VM ", destVm.getId(),
                    " predicted ETC ", predictedMaxEtc, " would not improve on source ETC ", sourceEtc,
                    ". Migration aborted.");
            return NO_MIGRATION;
        }

        Log.printlnConcat(now, ": [planner_v2] Migration plan: Cloudlet ", cloudletToMigrate.getCloudletId(),
                " from VM ", sourceVm.getId(), " -> VM ", destVm.getId());

        return new int[]{cloudletToMigrate.getCloudletId(), sourceVm.getId(), destVm.getId()};
    }

    /** Max estimated finish-time duration (ETC) across a VM's exec list. */
    private double maxEtc(GuestEntity vm, List<Cloudlet> execList, double now) {
        double max = 0.0;
        for (Cloudlet c : execList) {
            double etc = vm.getCloudletScheduler().getEstimatedFinishTime(c, now) - now;
            if (etc > max) {
                max = etc;
            }
        }
        return max;
    }

    /** Finds the VM with the lowest max-ETC matching the target LoadState, excluding the source. */
    private GuestEntity findDestination(List<GuestEntity> vmList, LoadState[] diagnosis,
                                         GuestEntity sourceVm, LoadState targetState, double now) {
        GuestEntity best = null;
        double bestEtc = Double.MAX_VALUE;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            GuestEntity vm = vmList.get(i);
            if (vm.getId() == sourceVm.getId()) continue;

            double etc = maxEtc(vm, vm.getCloudletScheduler().getCloudletExecList(), now);
            if (etc < bestEtc) {
                bestEtc = etc;
                best = vm;
            }
        }

        return best;
    }

    @Override
    public String inputGuid() {
        return "vm-etc-loadstate";
    }

    @Override
    public String outputGuid() {
        return "cloudlet-migration";
    }
}
