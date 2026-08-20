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
import java.util.ArrayList;
import java.util.Random;

public class planner_v40 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        List<GuestEntity> candidates = new ArrayList<GuestEntity>();
        List<Double> weights = new ArrayList<Double>();
        double totalWeight = 0;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (!readSpace.getVmCloudletList(vm).isEmpty()) continue;
            double util = readSpace.getVmCpuUtil(vm);
            double weight = 1.0 / (util + 0.01);
            candidates.add(vm);
            weights.add(weight);
            totalWeight += weight;
        }
        int vmId = -1;
        if (!candidates.isEmpty()) {
            Random random = new Random((long) (readSpace.getNow() * 1000.0));
            double pick = random.nextDouble() * totalWeight;
            double cumulative = 0;
            for (int i = 0; i < candidates.size(); i++) {
                cumulative += weights.get(i);
                if (pick <= cumulative) {
                    vmId = readSpace.getId(candidates.get(i));
                    break;
                }
            }
            if (vmId == -1) {
                vmId = readSpace.getId(candidates.get(candidates.size() - 1));
            }
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v40] Weighted Stochastic Reclamation destroying VM ", vmId, ".");
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-weighted-stochastic-reclaim";
    }

    @Override
    public String outputSemantic() {
        return "requestVmDestruction";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3004;
    }

}
