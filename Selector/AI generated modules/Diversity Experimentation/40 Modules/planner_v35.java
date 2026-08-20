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

public class planner_v35 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int targetId = -1;
        double worstWaste = -1;
        for (HostEntity h : hosts) {
            if (readSpace.isHostPoweredDown(h) || readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
            if (!readSpace.getVmListForHost(h).isEmpty()) continue;
            double waste = readSpace.getHostPower(h);
            if (waste > worstWaste) {
                worstWaste = waste;
                targetId = readSpace.getId(h);
            }
        }
        if (targetId == -1 && !hosts.isEmpty()) {
            targetId = readSpace.getId(hosts.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v35] Empty-Host Reaping powering down host ", targetId, " (idle power draw=", worstWaste, ").");
        return new int[]{targetId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-empty-host-scan";
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
