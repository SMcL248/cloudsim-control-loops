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

// Strategy: Backlog-Prioritized Vertical Scaling.
// Diagnosis is VM-level. Among VMs flagged OVERLOADED whose host still has
// MIPS headroom and whose MIPS tier can step up, picks the one carrying the
// largest total remaining cloudlet backlog and scales its MIPS to the next
// tier in place. Scaling vertically (no data movement, no migration cost)
// targets the single VM whose relief yields the greatest marginal drop in
// outstanding work, on the assumption that the biggest queue benefits most
// from a faster VM.
public class planner_v3 implements Planner<LoadState[], int[]> {

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
        double worstBacklog = -1;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (readSpace.getNextMipsTier(vm) < 0) {
                continue;
            }
            HostEntity host = hostOf(vm, readSpace);
            if (host == null || readSpace.getHostAvailableMips(host) <= 0) {
                continue;
            }
            double backlog = 0;
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                backlog += readSpace.getRemainingLength(cl);
            }
            if (backlog > worstBacklog) {
                worstBacklog = backlog;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] No scalable overloaded VM with host headroom found.");
            return new int[]{-1, -1};
        }

        double nextTierValue = readSpace.getNextMipsTier(target);
        int[] tiers = readSpace.getMipsTiers();
        int tierIndex = -1;
        for (int t = 0; t < tiers.length; t++) {
            if (tiers[t] == (int) nextTierValue) {
                tierIndex = t;
                break;
            }
        }

        if (tierIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] Could not resolve next MIPS tier index for VM ", readSpace.getId(target), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] Vertical MIPS scale-up: VM ", readSpace.getId(target), " to tier index ", tierIndex, ".");

        return new int[]{readSpace.getId(target), tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestMipsScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3005;
    }
}
