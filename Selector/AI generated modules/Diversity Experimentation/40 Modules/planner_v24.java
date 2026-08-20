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

public class planner_v24 implements Planner<LoadState[], int[]> {

    private int roundRobinCounter = 0;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int vmId = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) { vmId = readSpace.getId(vms.get(i)); break; }
        }
        List<HostEntity> healthyHosts = new ArrayList<HostEntity>();
        for (HostEntity h : readSpace.getAllHosts()) {
            if (!readSpace.isHostFailed(h) && !readSpace.isHostPermanentlyDead(h)) {
                healthyHosts.add(h);
            }
        }
        int targetHostId = -1;
        if (!healthyHosts.isEmpty()) {
            int index = roundRobinCounter % healthyHosts.size();
            roundRobinCounter++;
            targetHostId = readSpace.getId(healthyHosts.get(index));
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v24] Round-Robin Placement moving VM ", vmId, " to host ", targetHostId, ".");
        return new int[]{vmId, targetHostId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-round-robin-placement";
    }

    @Override
    public String outputSemantic() {
        return "requestVmMigration";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }

}
