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

public class planner_v15 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int[] ramTiers = readSpace.getRamTiers();
        int vmId = -1;
        int tierIndex = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            HostEntity host = null;
            for (HostEntity h : readSpace.getAllHosts()) {
                if (readSpace.getVmListForHost(h).contains(vm)) { host = h; break; }
            }
            if (host == null || readSpace.getHostAvailableRam(host) <= 0) continue;
            double nextRam = readSpace.getNextRamTier(vm);
            if (nextRam < 0) continue;
            for (int t = 0; t < ramTiers.length; t++) {
                if ((int) Math.round(nextRam) == ramTiers[t]) { tierIndex = t; break; }
            }
            if (tierIndex == -1) continue;
            vmId = readSpace.getId(vm);
            break;
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
            tierIndex = 0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v15] Host-Constrained RAM Scaling for VM ", vmId, " to tier ", tierIndex, ".");
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-host-constrained-ram";
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
