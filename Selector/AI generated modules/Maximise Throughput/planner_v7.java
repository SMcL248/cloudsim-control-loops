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

// Strategy: Sibling Headroom Rebalancing.
// Diagnosis is host-level. A host's PEs are a divisor of its fixed MIPS
// rating, not a countable slot pool -- so a VM sitting on more PEs than it
// currently needs quietly narrows the effective per-PE MIPS share available
// to its co-located siblings. On UNDERLOADED hosts, this variant looks
// across resident VMs for one whose allocated PE count exceeds its queued
// cloudlet count (over-provisioned parallelism sitting idle) and deallocates
// one surplus PE from the VM with the largest surplus on the most
// underloaded host. No data moves and no VM leaves its host; the intent is
// to widen the effective MIPS-per-PE share for that host's other resident
// VMs without a migration.
public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        GuestEntity bestVm = null;
        int bestSurplus = 0;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity h = hosts.get(i);
            if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h) || readSpace.isHostPoweredDown(h)) {
                continue;
            }
            for (GuestEntity vm : readSpace.getVmListForHost(h)) {
                if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                    continue;
                }
                int pes = readSpace.getVmNumberOfPes(vm);
                if (pes <= 1) {
                    continue;
                }
                int queued = readSpace.getVmCloudletList(vm).size();
                int surplus = pes - queued;
                if (surplus > bestSurplus) {
                    bestSurplus = surplus;
                    bestVm = vm;
                }
            }
        }

        if (bestVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] No over-provisioned VM found on any underloaded host.");
            return new int[]{-1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] PE deallocation: VM ", readSpace.getId(bestVm), " surplus ", bestSurplus, ".");

        return new int[]{readSpace.getId(bestVm)};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestPeDeallocation";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3009;
    }
}
