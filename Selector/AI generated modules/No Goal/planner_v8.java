package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 8: Feasibility-checked RAM scaling with candidate fallback.
 * Strategy: read VM-level diagnosis. For each OVERLOADED VM in turn,
 * resolve its next RAM tier and check whether its current host actually
 * has enough RAM headroom to satisfy the upgrade before committing. If a
 * candidate isn't feasible, fall through to the next OVERLOADED VM rather
 * than issuing a scaling request the host cannot honour.
 */
public class planner_v8 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int[] ramTiers = readSpace.getRamTiers();
        int considered = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);

            double nextRam = readSpace.getNextRamTier(vm);
            if (nextRam < 0) {
                continue;
            }

            int tierIndex = -1;
            for (int t = 0; t < ramTiers.length; t++) {
                if (ramTiers[t] == (int) nextRam) {
                    tierIndex = t;
                    break;
                }
            }
            if (tierIndex == -1) {
                continue;
            }

            HostEntity host = null;
            for (HostEntity h : hosts) {
                if (readSpace.getVmListForHost(h).contains(vm)) {
                    host = h;
                    break;
                }
            }
            if (host == null) {
                continue;
            }

            double requiredDelta = nextRam - readSpace.getVmRam(vm);
            double headroom = readSpace.getHostAvailableRam(host);
            if (requiredDelta > headroom) {
                Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ",
                        "VM " + readSpace.getId(vm) + " overloaded but host lacks RAM headroom, falling back to next candidate.");
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ",
                    "VM " + vmId + " overloaded and host has feasible RAM headroom, requesting RAM scale to tier " + tierIndex);
            return new int[]{vmId, tierIndex};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] ",
                "No feasible RAM scaling candidate found.");
        return new int[]{-1, -1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-overload-rampressure";
    }

    @Override
    public String outputSemantic() {
        return "ram-scaling-feasible";
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
