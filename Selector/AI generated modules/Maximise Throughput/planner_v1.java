package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

// Strategy: Best-Fit Consolidation.
// Diagnosis is host-level. Among OVERLOADED hosts, picks the one with the
// least remaining MIPS headroom fraction, migrates its single largest VM
// (biggest single relief per move) off, and lands it on the UNDERLOADED
// host that offers the tightest feasible fit (least leftover headroom after
// the move). Tight-fit placement preserves large contiguous free blocks on
// other hosts for future placements, a classic bin-packing consolidation
// approach aimed at keeping fewer hosts saturated so per-host throughput
// does not degrade under contention.
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity worstHost = null;
        double worstHeadroomRatio = Double.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            HostEntity h = hosts.get(i);
            if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h) || readSpace.isHostPoweredDown(h)) {
                continue;
            }
            double total = readSpace.getHostTotalMips(h);
            if (total <= 0) {
                continue;
            }
            double headroomRatio = readSpace.getHostAvailableMips(h) / total;
            if (headroomRatio < worstHeadroomRatio) {
                worstHeadroomRatio = headroomRatio;
                worstHost = h;
            }
        }

        if (worstHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] No overloaded host found, no migration issued.");
            return new int[]{-1, -1};
        }

        GuestEntity sourceVm = null;
        double largestMips = -1;

        for (GuestEntity vm : readSpace.getVmListForHost(worstHost)) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            double mips = readSpace.getVmRequestedMips(vm);
            if (mips > largestMips) {
                largestMips = mips;
                sourceVm = vm;
            }
        }

        if (sourceVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] Overloaded host ", readSpace.getId(worstHost), " has no eligible VM to migrate.");
            return new int[]{-1, -1};
        }

        HostEntity targetHost = null;
        double tightestLeftover = Double.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity h = hosts.get(i);
            if (h == worstHost) {
                continue;
            }
            if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h) || readSpace.isHostPoweredDown(h) || readSpace.isHostPoweringUp(h)) {
                continue;
            }
            if (!readSpace.canMigrateGuestToHost(h, sourceVm)) {
                continue;
            }
            double leftover = readSpace.getHostAvailableMips(h) - largestMips;
            if (leftover < 0) {
                continue;
            }
            if (leftover < tightestLeftover) {
                tightestLeftover = leftover;
                targetHost = h;
            }
        }

        if (targetHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] No best-fit underloaded host found for VM ", readSpace.getId(sourceVm), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] Consolidation migration: VM ", readSpace.getId(sourceVm), " from host ", readSpace.getId(worstHost), " to best-fit host ", readSpace.getId(targetHost), ".");

        return new int[]{readSpace.getId(sourceVm), readSpace.getId(targetHost)};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }
}
