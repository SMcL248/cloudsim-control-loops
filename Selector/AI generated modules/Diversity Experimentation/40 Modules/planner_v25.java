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

public class planner_v25 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int vmId = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm)) continue;
            vmId = readSpace.getId(vm);
            break;
        }
        int targetHostId = -1;
        if (vmId != -1) {
            GuestEntity vm = readSpace.getVmById(vmId);
            double bestHeadroom = -1;
            for (HostEntity h : readSpace.getAllHosts()) {
                if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
                if (!readSpace.canMigrateGuestToHost(h, vm)) continue;
                double headroom = readSpace.getHostAvailableMips(h);
                if (headroom > bestHeadroom) {
                    bestHeadroom = headroom;
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
        Log.printlnConcat(readSpace.getNow(), ": [planner_v25] Migration-Churn Avoidance moving stable VM ", vmId, " to host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-churn-avoidance";
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
