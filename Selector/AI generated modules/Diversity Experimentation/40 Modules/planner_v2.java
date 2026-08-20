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

public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        boolean overloadSeen = false;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) { overloadSeen = true; break; }
        }
        int targetId = -1;
        if (overloadSeen) {
            for (HostEntity h : hosts) {
                if (readSpace.isHostPoweredDown(h) && !readSpace.isHostPermanentlyDead(h)) {
                    targetId = readSpace.getId(h);
                    break;
                }
            }
        }
        if (targetId == -1 && !hosts.isEmpty()) {
            targetId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] Reserve Capacity Activation powering up host ", targetId, ".");
        return new int[]{targetId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-reserve-activation";
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
