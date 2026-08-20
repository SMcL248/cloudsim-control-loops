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

public class planner_v39 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        int n = Math.min(diagnosis.length, cloudlets.size());
        Cloudlet target = null;
        GuestEntity sourceVm = null;
        GuestEntity bestTargetVm = null;
        double bestImprovement = 0;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            Cloudlet cl = cloudlets.get(i);
            GuestEntity ownerVm = null;
            for (GuestEntity vm : readSpace.getVmList()) {
                if (readSpace.getVmCloudletList(vm).contains(cl)) { ownerVm = vm; break; }
            }
            if (ownerVm == null) continue;
            HostEntity ownerHost = null;
            for (HostEntity h : readSpace.getAllHosts()) {
                if (readSpace.getVmListForHost(h).contains(ownerVm)) { ownerHost = h; break; }
            }
            if (ownerHost == null) continue;
            double sourceHeadroom = readSpace.getHostAvailableMips(ownerHost);
            for (GuestEntity candidateVm : readSpace.getVmList()) {
                if (candidateVm == ownerVm) continue;
                HostEntity candidateHost = null;
                for (HostEntity h : readSpace.getAllHosts()) {
                    if (readSpace.getVmListForHost(h).contains(candidateVm)) { candidateHost = h; break; }
                }
                if (candidateHost == null || candidateHost == ownerHost) continue;
                if (readSpace.isHostFailed(candidateHost) || readSpace.isHostPermanentlyDead(candidateHost)) continue;
                double improvement = readSpace.getHostAvailableMips(candidateHost) - sourceHeadroom;
                if (improvement > bestImprovement) {
                    bestImprovement = improvement;
                    target = cl;
                    sourceVm = ownerVm;
                    bestTargetVm = candidateVm;
                }
            }
        }
        int cloudletId = -1;
        int fromVmId = -1;
        int toVmId = -1;
        if (target != null) {
            cloudletId = readSpace.getId(target);
            fromVmId = readSpace.getId(sourceVm);
            toVmId = readSpace.getId(bestTargetVm);
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
        Log.printlnConcat(readSpace.getNow(), ": [planner_v39] Strictly-Improving Migration moving cloudlet ", cloudletId, " (improvement=", bestImprovement, ") from VM ", fromVmId, " to VM ", toVmId, ".");
        return new int[]{cloudletId, fromVmId, toVmId};
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-loadstate-strictly-improving";
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
