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

public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        HostEntity sourceHost = null;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) { sourceHost = hosts.get(i); break; }
        }
        int vmId = -1;
        int targetHostId = -1;
        if (sourceHost != null) {
            List<GuestEntity> vmsOnHost = readSpace.getVmListForHost(sourceHost);
            if (!vmsOnHost.isEmpty()) {
                GuestEntity vm = vmsOnHost.get(0);
                vmId = readSpace.getId(vm);
                double bestHeadroom = -1;
                for (HostEntity h : hosts) {
                    if (h == sourceHost) continue;
                    if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
                    if (!readSpace.canMigrateGuestToHost(h, vm)) continue;
                    double headroom = readSpace.getHostAvailableMips(h);
                    if (headroom > bestHeadroom) {
                        bestHeadroom = headroom;
                        targetHostId = readSpace.getId(h);
                    }
                }
            }
        }
        if (vmId == -1 && !readSpace.getVmList().isEmpty()) {
            vmId = readSpace.getId(readSpace.getVmList().get(0));
        }
        if (targetHostId == -1 && !hosts.isEmpty()) {
            targetHostId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] Greedy Headroom Migration moving VM ", vmId, " to host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-greedy-headroom";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }

}
