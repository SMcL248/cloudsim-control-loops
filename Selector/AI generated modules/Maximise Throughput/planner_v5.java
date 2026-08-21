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

// Strategy: RAM-Headroom Concurrency Unlock.
// Diagnosis is VM-level. Treats an OVERLOADED VM's queued-cloudlet count as
// a proxy for concurrency pressure: a VM juggling many simultaneously
// resident cloudlets needs memory headroom to keep them all resident and
// runnable rather than stalling admission. Among OVERLOADED VMs whose RAM
// tier can step up and whose host has RAM headroom to support it, scales
// the RAM of whichever VM has the most cloudlets queued, on the reasoning
// that memory starvation -- not compute -- is what is throttling that VM's
// admitted concurrency.
public class planner_v5 implements Planner<LoadState[], int[]> {

    private HostEntity hostOf(GuestEntity vm, ReadSpace readSpace) {
        for (HostEntity h : readSpace.getAllHosts()) {
            for (GuestEntity v : readSpace.getVmListForHost(h)) {
                if (readSpace.getId(v) == readSpace.getId(vm)) {
                    return h;
                }
            }
        }
        return null;
    }

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        int mostQueued = -1;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (readSpace.getNextRamTier(vm) < 0) {
                continue;
            }
            HostEntity host = hostOf(vm, readSpace);
            if (host == null || readSpace.getHostAvailableRam(host) <= 0) {
                continue;
            }
            int queued = readSpace.getVmCloudletList(vm).size();
            if (queued > mostQueued) {
                mostQueued = queued;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] No scalable overloaded VM with host RAM headroom found.");
            return new int[]{-1, -1};
        }

        double nextRam = readSpace.getNextRamTier(target);
        int[] tiers = readSpace.getRamTiers();
        int tierIndex = -1;
        for (int t = 0; t < tiers.length; t++) {
            if (tiers[t] == (int) nextRam) {
                tierIndex = t;
                break;
            }
        }

        if (tierIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] Could not resolve next RAM tier index for VM ", readSpace.getId(target), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] RAM scale-up: VM ", readSpace.getId(target), " to tier index ", tierIndex, ".");

        return new int[]{readSpace.getId(target), tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestRamScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3006;
    }
}
