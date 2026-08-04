package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level planner. diagnosis[i] is the load state of readSpace.getAllHosts().get(i).
// Goal: maximise throughput / minimise makespan.
// Strategy: when overloaded hosts outnumber underloaded ones (no spare
// capacity exists to migrate into), the datacenter is saturated rather than
// merely imbalanced. Request a new, largest-tier VM to add fresh parallel
// processing capacity instead of shuffling load between already-busy hosts.
// The datacenter id is read off an existing VM on an overloaded host.
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v3";
    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3003;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/host size mismatch, no-op");
            return new int[]{-1, -1, -1};
        }

        int overloadedCount = 0;
        int underloadedCount = 0;
        GuestEntity referenceVm = null;

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
                if (referenceVm == null) {
                    List<GuestEntity> guests = readSpace.getVmListForHost(hosts.get(i));
                    if (!guests.isEmpty()) {
                        referenceVm = guests.get(0);
                    }
                }
            } else if (diagnosis[i] == LoadState.UNDERLOADED) {
                underloadedCount++;
            }
        }

        if (overloadedCount == 0 || overloadedCount <= underloadedCount || referenceVm == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] datacenter not saturated (overloaded=",
                    overloadedCount, ", underloaded=", underloadedCount, "), no-op");
            return new int[]{-1, -1, -1};
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        if (mipsTiers == null || mipsTiers.length == 0) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no mips tiers available, no-op");
            return new int[]{-1, -1, -1};
        }

        // No dedicated storage-size-tier accessor is exposed on ReadSpace, so
        // the top resource tier index doubles as the size-tier index (i.e.
        // request a "large" VM class on both axes).
        int topTierIndex = mipsTiers.length - 1;
        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(referenceVm));

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan create large VM (tier ", topTierIndex,
                ") in datacenter ", datacenterId, " to relieve saturation");
        return new int[]{topTierIndex, topTierIndex, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "host-mips-congestion-saturation";
    }

    @Override
    public String outputSemantic() {
        return "requestvmcreation";
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
