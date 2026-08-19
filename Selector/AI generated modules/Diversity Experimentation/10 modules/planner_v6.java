package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: Host-headroom-guarded RAM scale-up.
// Reacts to VMs whose diagnosis reflects RAM pressure rather than CPU pressure.
// Before committing to a RAM tier increase, this planner checks whether the VM's
// current host actually has enough spare RAM to absorb the increment. If the host
// cannot support it, the request is deferred rather than issued blindly, avoiding
// pushing a host into resource exhaustion.
public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ", "checking RAM-pressured VMs for guarded scale-up");

        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int[] ramTiers = readSpace.getRamTiers();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            double nextRamTierValue = readSpace.getNextRamTier(vm);
            if (nextRamTierValue < 0) {
                continue;
            }

            double increment = nextRamTierValue - readSpace.getVmRam(vm);
            if (increment <= 0) {
                continue;
            }

            HostEntity host = findHostOf(readSpace, hosts, vm);
            if (host == null) {
                continue;
            }

            if (readSpace.getHostAvailableRam(host) < increment) {
                Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ", "deferring RAM scale-up, insufficient host headroom for vmId=" + readSpace.getId(vm));
                continue;
            }

            int tierIndex = indexOfTier(ramTiers, nextRamTierValue);
            if (tierIndex < 0) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ", "scaling RAM for vmId=" + vmId + " to tier index=" + tierIndex);
            return new int[] { vmId, tierIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ", "no RAM-pressured VM was safe to scale this cycle");
        return null;
    }

    private HostEntity findHostOf(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        for (HostEntity host : hosts) {
            if (readSpace.getVmListForHost(host).contains(vm)) {
                return host;
            }
        }
        return null;
    }

    private int indexOfTier(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-ramheadroom-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-ram-scaleup-guarded";
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
