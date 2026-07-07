package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

/**
 * planner_v4 - VM Backlog-Time Balancer
 *
 * Metric: "backlog" = total remaining cloudlet length divided by the VM's
 * MIPS capacity (totalRemainingLength / vm.getMips()), an estimate of how
 * long the VM's queue would take to drain running alone. Unlike raw
 * length, this normalises for heterogeneous VM capacity.
 *
 * Strategy:
 *   1. Among OVERLOADED VMs with a non-empty exec list, select the one
 *      with the highest backlog as the migration source.
 *   2. From the source VM, migrate its single heaviest cloudlet (largest
 *      remaining length), the biggest contributor to the backlog.
 *   3. Among UNDERLOADED VMs (falling back to BALANCED if none exist),
 *      excluding the source, select the destination with the lowest
 *      backlog.
 *   4. Feasibility check (length/mips-based, not demand-based): only
 *      migrate if the destination's backlog after accepting the cloudlet
 *      would not exceed the source's original backlog.
 *
 * Input  GUID : vm-backlog-loadstate
 * Output GUID : cloudlet-migration
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final int[] NO_MIGRATION = new int[]{-1, -1, -1};

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vmList = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vmList.size()) {
            Log.printlnConcat(now, ": [planner_v4] Diagnosis length mismatch (expected ",
                    vmList.size(), ", got ", (diagnosis == null ? "null" : diagnosis.length), "). Aborting.");
            return NO_MIGRATION;
        }

        // --- Step 1: select the source VM with the highest backlog ---
        GuestEntity sourceVm = null;
        double sourceBacklog = -1.0;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vmList.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
            if (execList.isEmpty()) continue;

            double backlog = backlog(vm, execList);
            if (backlog > sourceBacklog) {
                sourceBacklog = backlog;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(now, ": [planner_v4] No overloaded VM with migratable cloudlets. No migration needed.");
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
            Log.printlnConcat(now, ": [planner_v4] Source VM ", sourceVm.getId(), " has no cloudlets to migrate.");
            return NO_MIGRATION;
        }

        // --- Step 3: find a destination VM by lowest backlog ---
        GuestEntity destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.UNDERLOADED);
        if (destVm == null) {
            destVm = findDestination(vmList, diagnosis, sourceVm, LoadState.BALANCED);
        }

        if (destVm == null) {
            Log.printlnConcat(now, ": [planner_v4] No suitable destination VM found. Migration aborted.");
            return NO_MIGRATION;
        }

        // --- Step 4: length/mips-based feasibility check ---
        double destMips = destVm.getMips();
        if (destMips <= 0.0) {
            Log.printlnConcat(now, ": [planner_v4] Destination VM ", destVm.getId(),
                    " has no usable MIPS capacity. Migration aborted.");
            return NO_MIGRATION;
        }

        double destLength = totalRemainingLength(destVm.getCloudletScheduler().getCloudletExecList());
        double predictedBacklog = (destLength + cloudletToMigrate.getRemainingCloudletLength()) / destMips;

        if (predictedBacklog > sourceBacklog) {
            Log.printlnConcat(now, ": [planner_v4] Destination VM ", destVm.getId(),
                    " predicted backlog ", predictedBacklog, " would not improve on source backlog ", sourceBacklog,
                    ". Migration aborted.");
            return NO_MIGRATION;
        }

        Log.printlnConcat(now, ": [planner_v4] Migration plan: Cloudlet ", cloudletToMigrate.getCloudletId(),
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

    /** Backlog = total remaining length / VM MIPS capacity (0 if MIPS unavailable). */
    private double backlog(GuestEntity vm, List<Cloudlet> execList) {
        double mips = vm.getMips();
        if (mips <= 0.0) {
            return 0.0;
        }
        return totalRemainingLength(execList) / mips;
    }

    /** Finds the VM with the lowest backlog matching the target LoadState, excluding the source. */
    private GuestEntity findDestination(List<GuestEntity> vmList, LoadState[] diagnosis,
                                         GuestEntity sourceVm, LoadState targetState) {
        GuestEntity best = null;
        double bestBacklog = Double.MAX_VALUE;

        for (int i = 0; i < vmList.size(); i++) {
            if (diagnosis[i] != targetState) continue;
            GuestEntity vm = vmList.get(i);
            if (vm.getId() == sourceVm.getId()) continue;

            double backlog = backlog(vm, vm.getCloudletScheduler().getCloudletExecList());
            if (backlog < bestBacklog) {
                bestBacklog = backlog;
                best = vm;
            }
        }

        return best;
    }

    @Override
    public String inputGuid() {
        return "vm-backlog-loadstate";
    }

    @Override
    public String outputGuid() {
        return "cloudlet-migration";
    }
}
