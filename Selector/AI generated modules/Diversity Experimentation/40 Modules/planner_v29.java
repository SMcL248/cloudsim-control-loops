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

public class planner_v29 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int n = Math.min(diagnosis.length, cloudlets.size());
        Cloudlet target = null;
        double worstFinish = -1;
        GuestEntity targetOwner = null;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            Cloudlet cl = cloudlets.get(i);
            GuestEntity ownerVm = null;
            for (GuestEntity vm : readSpace.getVmList()) {
                if (readSpace.getVmCloudletList(vm).contains(cl)) { ownerVm = vm; break; }
            }
            if (ownerVm == null) continue;
            double finish = readSpace.getCloudletEstimatedFinishTime(ownerVm, cl);
            if (finish > worstFinish) {
                worstFinish = finish;
                target = cl;
                targetOwner = ownerVm;
            }
        }
        int cloudletId = -1;
        int fromVmId = -1;
        int toVmId = -1;
        if (target != null && targetOwner != null) {
            cloudletId = readSpace.getId(target);
            fromVmId = readSpace.getId(targetOwner);
            double bestHeadroom = -1;
            for (GuestEntity vm : readSpace.getVmList()) {
                if (vm == targetOwner) continue;
                double headroom = readSpace.getVmRequestedMips(vm) - (readSpace.getVmCpuUtil(vm) * readSpace.getVmRequestedMips(vm));
                if (headroom > bestHeadroom) {
                    bestHeadroom = headroom;
                    toVmId = readSpace.getId(vm);
                }
            }
        }
        List<GuestEntity> vmList = readSpace.getVmList();
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
        Log.printlnConcat(readSpace.getNow(), ": [planner_v29] Finish-Time-Risk Migration moving cloudlet ", cloudletId, " (est. finish=", worstFinish, ") from VM ", fromVmId, " to VM ", toVmId, ".");
        return new int[]{cloudletId, fromVmId, toVmId};
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-finish-time-risk";
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
