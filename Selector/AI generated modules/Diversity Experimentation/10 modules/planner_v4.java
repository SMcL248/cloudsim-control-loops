package org.cloudbus.cloudsim.examples;

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

// Sustained-Overload Scale-Out Planner.
// Computes the fraction of VMs flagged OVERLOADED in the VM-level
// LoadState[]. When a majority of VMs are overloaded, requests creation of
// a new, moderately sized VM (middle MIPS/RAM/BW tier) in the same
// datacenter as the existing fleet, to horizontally absorb demand.
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3003;
    private static final double OVERLOAD_RATIO_THRESHOLD = 0.5;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int sampleSize = Math.min(diagnosis.length, vms.size());
        int overloadedCount = 0;
        for (int i = 0; i < sampleSize; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        double ratio = sampleSize == 0 ? 0.0 : (double) overloadedCount / sampleSize;
        if (ratio < OVERLOAD_RATIO_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] overload ratio ", ratio, " below scale-out threshold");
            return new int[0];
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        if (mipsTiers.length == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] no mips tiers configured, cannot size new vm");
            return new int[0];
        }

        int mediumTier = mipsTiers.length / 2;
        int datacenterId = 0;
        if (!vms.isEmpty()) {
            Integer dc = readSpace.getDatacenterFor(readSpace.getId(vms.get(0)));
            if (dc != null) {
                datacenterId = dc;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] sustained overload detected, requesting new medium vm in datacenter ", datacenterId);
        return new int[] { mediumTier, mediumTier, datacenterId };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
