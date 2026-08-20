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

public class planner_v23 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int vmId = -1;
        double bestScore = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            double util = readSpace.getVmCpuUtil(vm);
            double score = Math.abs(util - 0.5);
            if (score > bestScore) {
                bestScore = score;
                vmId = readSpace.getId(vm);
            }
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
        Log.printlnConcat(readSpace.getNow(), ": [planner_v23] Balance-Distance Scoring Migration moving VM ", vmId, " (score=", bestScore, ") to host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-balance-distance-score";
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
