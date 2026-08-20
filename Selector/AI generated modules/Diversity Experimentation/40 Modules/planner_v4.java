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

public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        int targetId = -1;
        double bestSavings = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            HostEntity h = hosts.get(i);
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            if (readSpace.isHostPoweredDown(h) || readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
            double estimate = readSpace.getHostEnergyEstimate(h, 1.0, 0.0, 1.0);
            if (estimate > bestSavings) {
                bestSavings = estimate;
                targetId = readSpace.getId(h);
            }
        }
        if (targetId == -1 && !hosts.isEmpty()) {
            targetId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] Energy-Savings-Threshold Power-Down targeting host ", targetId, " with estimated saving ", bestSavings, ".");
        return new int[]{targetId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-energy-savings";
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
