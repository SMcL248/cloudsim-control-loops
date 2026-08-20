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

public class planner_v8 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        int overloadedCount = 0;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) overloadedCount++;
        }
        int targetId = -1;
        if (n > 0 && overloadedCount * 2 > n) {
            double bestCapacity = -1;
            for (HostEntity h : hosts) {
                if (!readSpace.isHostPoweredDown(h) || readSpace.isHostPermanentlyDead(h)) continue;
                double capacity = readSpace.getHostMaxPower(h);
                if (capacity > bestCapacity) {
                    bestCapacity = capacity;
                    targetId = readSpace.getId(h);
                }
            }
        }
        if (targetId == -1 && !hosts.isEmpty()) {
            targetId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] Worst-Case Capacity Injection powering up host ", targetId, " (", overloadedCount, "/", n, " hosts overloaded).");
        return new int[]{targetId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-worst-case-injection";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerUp";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3011;
    }

}
