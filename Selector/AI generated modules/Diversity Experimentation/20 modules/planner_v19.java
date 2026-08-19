package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class planner_v19 implements Planner<LoadState[], int[]> {

    private static final double PLANNING_HORIZON = 60.0;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        HostEntity bestHost = null;
        double bestSavings = -Double.MAX_VALUE;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips <= 0) {
                continue;
            }
            double currentUtil = (totalMips - readSpace.getHostAvailableMips(host)) / totalMips;
            double savings = readSpace.getHostEnergyEstimate(host, currentUtil, 0.0, PLANNING_HORIZON);
            if (savings > bestSavings) {
                bestSavings = savings;
                bestHost = host;
            }
        }

        if (bestHost == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v19] no underloaded host with positive projected energy savings");
            return new int[0];
        }

        int hostId = readSpace.getId(bestHost);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v19] powering down host ", hostId, " for projected energy savings=", bestSavings);
        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-underload-energy-savings";
    }

    @Override
    public String outputSemantic() {
        return "host-power-down";
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
