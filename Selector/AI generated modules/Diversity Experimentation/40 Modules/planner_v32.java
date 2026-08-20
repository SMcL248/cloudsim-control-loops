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

public class planner_v32 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int n = Math.min(diagnosis.length, cloudlets.size());
        List<GuestEntity> vmList = readSpace.getVmList();
        double meanCount = 0;
        if (!vmList.isEmpty()) {
            int total = 0;
            for (GuestEntity vm : vmList) {
                total += readSpace.getVmCloudletList(vm).size();
            }
            meanCount = (double) total / vmList.size();
        }
        Cloudlet target = null;
        GuestEntity sourceVm = null;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            Cloudlet cl = cloudlets.get(i);
            GuestEntity ownerVm = null;
            for (GuestEntity vm : vmList) {
                if (readSpace.getVmCloudletList(vm).contains(cl)) { ownerVm = vm; break; }
            }
            if (ownerVm == null) continue;
            if (readSpace.getVmCloudletList(ownerVm).size() <= meanCount) continue;
            target = cl;
            sourceVm = ownerVm;
            break;
        }
        int cloudletId = -1;
        int fromVmId = -1;
        int toVmId = -1;
        if (target != null) {
            cloudletId = readSpace.getId(target);
            fromVmId = readSpace.getId(sourceVm);
            int fewest = Integer.MAX_VALUE;
            for (GuestEntity vm : vmList) {
                if (vm == sourceVm) continue;
                int count = readSpace.getVmCloudletList(vm).size();
                if (count < fewest) {
                    fewest = count;
                    toVmId = readSpace.getId(vm);
                }
            }
        }
        if (cloudletId == -1 && !cloudlets.isEmpty()) {
            cloudletId = readSpace.getId(cloudlets.get(0));
        }
        if (fromVmId == -1 && !vmList.isEmpty()) {
            fromVmId = readSpace.getId(vmList.get(0));
        }
        if (toVmId == -1 && vmList.size() > 1) {
            toVmId = readSpace.getId(vmList.get(1));
        } else if (toVmId == -1 && !vmList.isEmpty()) {
            toVmId = readSpace.getId(vmList.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v32] Cloudlet-Count Fairness Rebalancing moving cloudlet ", cloudletId, " from VM ", fromVmId, " (mean=", meanCount, ") to VM ", toVmId, ".");
        return new int[]{cloudletId, fromVmId, toVmId};
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-count-fairness";
    }

    @Override
    public String outputSemantic() {
        return "moveCloudlet";
    }

    @Override
    public int inputGuid() {
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }

}
