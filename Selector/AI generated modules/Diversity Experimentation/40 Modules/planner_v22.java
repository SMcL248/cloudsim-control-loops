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

public class planner_v22 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int overloadedCount = 0;
        int underloadedCount = 0;
        int firstOverloadedVmId = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
                if (firstOverloadedVmId == -1) firstOverloadedVmId = readSpace.getId(vms.get(i));
            } else if (diagnosis[i] == LoadState.UNDERLOADED) {
                underloadedCount++;
            }
        }
        int[] mipsTiers = readSpace.getMipsTiers();
        int midTier = mipsTiers.length > 0 ? mipsTiers.length / 2 : 0;
        int datacenterId = 0;
        if (firstOverloadedVmId != -1) {
            Integer dc = readSpace.getDatacenterFor(firstOverloadedVmId);
            if (dc != null) datacenterId = dc;
        }
        boolean shouldScale = overloadedCount > underloadedCount;
        Log.printlnConcat(readSpace.getNow(), ": [planner_v22] Overload-Triggered Scale-Out shouldScale=", shouldScale, " requesting tier ", midTier, " in datacenter ", datacenterId, ".");
        return new int[]{midTier, midTier, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-overload-scaleout";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }

}
