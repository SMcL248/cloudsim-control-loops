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

public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        int bestIndex = -1;
        int bestClusterScore = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            int clusterScore = 0;
            for (int j = Math.max(0, i - 2); j <= Math.min(n - 1, i + 2); j++) {
                if (j != i && diagnosis[j] == LoadState.OVERLOADED) clusterScore++;
            }
            if (clusterScore > bestClusterScore) {
                bestClusterScore = clusterScore;
                bestIndex = i;
            }
        }
        int vmId = -1;
        int targetHostId = -1;
        if (bestIndex != -1) {
            HostEntity sourceHost = hosts.get(bestIndex);
            List<GuestEntity> vmsOnHost = readSpace.getVmListForHost(sourceHost);
            if (!vmsOnHost.isEmpty()) {
                GuestEntity vm = vmsOnHost.get(0);
                vmId = readSpace.getId(vm);
                int farthestIndex = -1;
                int farthestDistance = -1;
                for (int j = 0; j < n; j++) {
                    if (diagnosis[j] != LoadState.BALANCED) continue;
                    HostEntity candidate = hosts.get(j);
                    if (readSpace.isHostFailed(candidate) || readSpace.isHostPermanentlyDead(candidate)) continue;
                    if (!readSpace.canMigrateGuestToHost(candidate, vm)) continue;
                    int distance = Math.abs(j - bestIndex);
                    if (distance > farthestDistance) {
                        farthestDistance = distance;
                        farthestIndex = j;
                    }
                }
                if (farthestIndex != -1) {
                    targetHostId = readSpace.getId(hosts.get(farthestIndex));
                }
            }
        }
        if (vmId == -1 && !readSpace.getVmList().isEmpty()) {
            vmId = readSpace.getId(readSpace.getVmList().get(0));
        }
        if (targetHostId == -1 && !hosts.isEmpty()) {
            targetHostId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] Spatial-Cluster Contention Migration relocating VM ", vmId, " away from cluster to host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-spatial-cluster";
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
