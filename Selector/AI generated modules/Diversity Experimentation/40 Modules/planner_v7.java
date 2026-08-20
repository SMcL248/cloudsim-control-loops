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

public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        int underloadedCount = 0;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.UNDERLOADED) underloadedCount++;
        }
        int targetId = -1;
        if (n > 0 && underloadedCount * 2 > n) {
            int fewestVms = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                HostEntity h = hosts.get(i);
                if (diagnosis[i] != LoadState.UNDERLOADED) continue;
                if (readSpace.isHostPoweredDown(h) || readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
                int vmCount = readSpace.getVmListForHost(h).size();
                if (vmCount < fewestVms) {
                    fewestVms = vmCount;
                    targetId = readSpace.getId(h);
                }
            }
        }
        if (targetId == -1 && !hosts.isEmpty()) {
            targetId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] Majority-Vote Consolidation powering down host ", targetId, " (", underloadedCount, "/", n, " hosts underloaded).");
        return new int[]{targetId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-majority-vote";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerDown";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3010;
    }

}
