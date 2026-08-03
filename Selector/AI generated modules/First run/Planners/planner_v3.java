package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Planner v3 - Capacity expansion via host power-up (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-host LoadState[] (index i corresponds to
 *   readSpace.getAllHosts().get(i)). When at least half of the (non-dead)
 *   hosts are OVERLOADED, the fleet is short on capacity; wakes up the
 *   first powered-down host that is not permanently dead so it can accept
 *   guests and relieve the overloaded hosts.
 *
 * Input semantic  : host-loadstate-saturation (GUID 2200)
 * Output semantic : host-powerup              (GUID 3015, requestHostPowerUp)
 */
public class planner_v3 implements Planner<LoadState[], int[]> {

    private static final double SATURATION_RATIO = 0.5;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [planner_v3] Diagnosis/host size mismatch. No-op.");
            return new int[]{-1};
        }

        int liveCount = 0;
        int overloadedCount = 0;
        for (int i = 0; i < hosts.size(); i++) {
            if (readSpace.isHostPermanentlyDead(hosts.get(i))) continue;
            liveCount++;
            if (diagnosis[i] == LoadState.OVERLOADED) overloadedCount++;
        }

        if (liveCount == 0 || (double) overloadedCount / liveCount < SATURATION_RATIO) {
            Log.printlnConcat(now, ": [planner_v3] Fleet not saturated (", overloadedCount,
                    "/", liveCount, " overloaded). No-op.");
            return new int[]{-1};
        }

        for (HostEntity host : hosts) {
            if (readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host) && !readSpace.isHostPoweringUp(host)) {
                int hostId = readSpace.getId(host);
                Log.printlnConcat(now, ": [planner_v3] Fleet saturated (", overloadedCount,
                        "/", liveCount, "). Plan power-up for host ", hostId);
                return new int[]{hostId};
            }
        }

        Log.printlnConcat(now, ": [planner_v3] Fleet saturated but no powered-down host available. No-op.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-saturation";
    }

    @Override
    public String outputSemantic() {
        return "host-powerup";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3015;
    }
}
