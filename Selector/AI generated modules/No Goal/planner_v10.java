package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 10: Contention-aware PE reclamation.
 * Strategy: read VM-level diagnosis. An UNDERLOADED VM holding more than
 * one PE is only a deallocation candidate if it shares a host with at
 * least one OVERLOADED neighbor - reclaiming spare capacity specifically
 * where it can relieve local contention, rather than trimming any
 * underloaded VM indiscriminately.
 */
public class planner_v10 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int considered = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
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

            boolean neighborContention = false;
            for (GuestEntity neighbor : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(neighbor) == readSpace.getId(vm)) {
                    continue;
                }
                int neighborIndex = vms.indexOf(neighbor);
                if (neighborIndex >= 0 && neighborIndex < considered
                        && diagnosis[neighborIndex] == LoadState.OVERLOADED) {
                    neighborContention = true;
                    break;
                }
            }

            if (!neighborContention) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ",
                    "VM " + vmId + " underloaded with spare PEs and contended host neighbors, reclaiming a PE to relieve host pressure.");
            return new int[]{vmId};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] ",
                "No contended underloaded VM with reclaimable PEs found.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-underload-contention";
    }

    @Override
    public String outputSemantic() {
        return "pe-deallocation-reclaim";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3009;
    }
}
