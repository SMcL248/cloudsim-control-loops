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

// Strategy: Volatility-Profiled Bandwidth Unlock.
// Diagnosis is VM-level. Rather than treating every OVERLOADED VM the same,
// this variant profiles each candidate's utilisation stability using its
// rolling mean and MAD (getVmUtilizationMean / getVmUtilizationMad) and
// computes a coefficient of variation (MAD / mean). A high coefficient
// indicates a bursty, spiky workload pattern -- the kind associated with
// intermittent data movement rather than steady compute saturation -- so
// the VM with the most volatile profile among OVERLOADED, scalable VMs
// (with host BW headroom available) is chosen for a bandwidth tier bump
// instead of a compute-oriented remedy. This is the only variant that picks
// its target resource dimension from a workload-shape signal rather than a
// queue-length or capacity proxy.
public class planner_v6 implements Planner<LoadState[], int[]> {

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
        double highestCv = -1;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (readSpace.getNextBwTier(vm) < 0) {
                continue;
            }
            HostEntity host = hostOf(vm, readSpace);
            if (host == null || readSpace.getHostAvailableBw(host) <= 0) {
                continue;
            }
            double mean = readSpace.getVmUtilizationMean(vm);
            if (mean <= 0) {
                continue;
            }
            double mad = readSpace.getVmUtilizationMad(vm);
            double cv = mad / mean;
            if (cv > highestCv) {
                highestCv = cv;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] No scalable overloaded VM with a volatile utilisation profile found.");
            return new int[]{-1, -1};
        }

        double nextBw = readSpace.getNextBwTier(target);
        int[] tiers = readSpace.getBwTiers();
        int tierIndex = -1;
        for (int t = 0; t < tiers.length; t++) {
            if (tiers[t] == (int) nextBw) {
                tierIndex = t;
                break;
            }
        }

        if (tierIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] Could not resolve next BW tier index for VM ", readSpace.getId(target), ".");
            return new int[]{-1, -1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] Bandwidth scale-up: VM ", readSpace.getId(target), " coefficient of variation ", highestCv, " to tier index ", tierIndex, ".");

        return new int[]{readSpace.getId(target), tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestBwScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3007;
    }
}
