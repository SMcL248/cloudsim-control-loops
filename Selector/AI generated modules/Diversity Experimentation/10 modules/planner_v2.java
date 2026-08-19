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

// Congestion-Triggered Power-Up Planner.
// Computes the fraction of hosts flagged OVERLOADED in the host-level
// LoadState[]. If that fraction crosses a threshold, brings a powered-down
// host back online, preferring the one with the greatest total MIPS capacity
// so the added headroom relieves the most congestion.
public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3011;
    private static final double OVERLOAD_RATIO_THRESHOLD = 0.34;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int sampleSize = Math.min(diagnosis.length, hosts.size());
        int overloadedCount = 0;
        for (int i = 0; i < sampleSize; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        double ratio = sampleSize == 0 ? 0.0 : (double) overloadedCount / sampleSize;
        if (ratio < OVERLOAD_RATIO_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] overload ratio ", ratio, " below threshold, no power-up needed");
            return new int[0];
        }

        HostEntity target = null;
        double bestCapacity = -1.0;
        for (HostEntity host : hosts) {
            if (readSpace.isHostPermanentlyDead(host) || readSpace.isHostFailed(host)) {
                continue;
            }
            if (!readSpace.isHostPoweredDown(host)) {
                continue;
            }
            double capacity = readSpace.getHostTotalMips(host);
            if (capacity > bestCapacity) {
                bestCapacity = capacity;
                target = host;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] congestion detected but no powered-down host available");
            return new int[0];
        }

        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] powering up host ", hostId, " to relieve congestion");
        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerUp";
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
