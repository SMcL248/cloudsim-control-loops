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

public class planner_v10 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = Math.min(diagnosis.length, hosts.size());
        HostEntity overloadedHost = null;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) { overloadedHost = hosts.get(i); break; }
        }
        boolean ramConstrained = false;
        if (overloadedHost != null) {
            ramConstrained = true;
            for (HostEntity h : hosts) {
                if (h == overloadedHost) continue;
                if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h)) continue;
                if (readSpace.getHostAvailableRam(h) > readSpace.getHostTotalRam(h) * 0.3) {
                    ramConstrained = false;
                    break;
                }
            }
        }
        int[] mipsTiers = readSpace.getMipsTiers();
        int[] ramTiers = readSpace.getRamTiers();
        int midTier = Math.max(0, Math.min(mipsTiers.length - 1, ramTiers.length / 2));
        int datacenterId = 0;
        if (overloadedHost != null && !readSpace.getVmListForHost(overloadedHost).isEmpty()) {
            GuestEntity anchorVm = readSpace.getVmListForHost(overloadedHost).get(0);
            Integer dc = readSpace.getDatacenterFor(readSpace.getId(anchorVm));
            if (dc != null) datacenterId = dc;
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] RAM-Pressure Scale-Out ramConstrained=", ramConstrained, " requesting new VM at tier ", midTier, " in datacenter ", datacenterId, ".");
        return new int[]{midTier, midTier, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-ram-pressure";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }

}
