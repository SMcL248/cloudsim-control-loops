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

public class planner_v17 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int vmId = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            HostEntity host = null;
            for (HostEntity h : readSpace.getAllHosts()) {
                if (readSpace.getVmListForHost(h).contains(vm)) { host = h; break; }
            }
            if (host == null || !readSpace.hostHasFreePe(host)) continue;
            vmId = readSpace.getId(vm);
            break;
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v17] Host-Gated PE Allocation for VM ", vmId, ".");
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-host-gated-pe-alloc";
    }

    @Override
    public String outputSemantic() {
        return "requestPeAllocation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3008;
    }

}
