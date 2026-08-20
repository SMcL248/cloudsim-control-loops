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

public class planner_v20 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int vmId = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) { vmId = readSpace.getId(vms.get(i)); break; }
        }
        int targetHostId = -1;
        if (vmId != -1) {
            GuestEntity vm = readSpace.getVmById(vmId);
            int mostPes = -1;
            for (HostEntity h : readSpace.getAllHosts()) {
                if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
                if (!readSpace.canMigrateGuestToHost(h, vm)) continue;
                int peCount = readSpace.getHostPeCount(h);
                if (peCount > mostPes) {
                    mostPes = peCount;
                    targetHostId = readSpace.getId(h);
                }
            }
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        List<HostEntity> hosts = readSpace.getAllHosts();
        if (targetHostId == -1 && !hosts.isEmpty()) {
            targetHostId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v20] PE-Capacity-Class Migration moving VM ", vmId, " to high-PE host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-pe-capacity-class";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }

}
