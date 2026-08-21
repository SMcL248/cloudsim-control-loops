package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 7 - Systemic Saturation Relief.
 *
 * Strategy: average power is energy over makespan, so a longer makespan
 * caused by sustained, system-wide saturation can hurt the average as much
 * as raw energy draw does. This variant deliberately spends power (a host
 * power-up, including its power-on spike) but only when a large supermajority
 * of currently active hosts are simultaneously OVERLOADED with zero free PE
 * headroom - true systemic saturation, not a local blip. In that regime,
 * adding capacity shortens the remaining makespan more than the extra host's
 * energy costs, lowering the time-averaged power. Among powered-down
 * candidates it picks the highest-capacity host, to get the most relief per
 * power-on event and minimise how often this trade has to be made again.
 */
public class planner_v7 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3011;

    private static final double SATURATION_RATIO_THRESHOLD = 0.8;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(diagnosis.length, hosts.size());

        int activeCount = 0;
        int saturatedCount = 0;
        for (int i = 0; i < limit; i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            activeCount++;
            if (diagnosis[i] == LoadState.OVERLOADED && !readSpace.hostHasFreePe(host)) {
                saturatedCount++;
            }
        }

        int[] noOp = new int[]{-1};
        double saturationRatio = activeCount == 0 ? 0.0 : (double) saturatedCount / activeCount;
        if (activeCount == 0 || saturationRatio < SATURATION_RATIO_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] saturation ratio ", saturationRatio,
                    " below threshold, emitting no-op");
            return noOp;
        }

        HostEntity bestCandidate = null;
        double bestMips = -1.0;
        for (HostEntity host : hosts) {
            if (readSpace.isHostPermanentlyDead(host) || readSpace.isHostFailed(host)) {
                continue;
            }
            if (!readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips > bestMips) {
                bestMips = totalMips;
                bestCandidate = host;
            }
        }

        if (bestCandidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] system saturated but no powered-down host available, emitting no-op");
            return noOp;
        }

        int hostId = readSpace.getId(bestCandidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] system-wide saturation detected, powering up highest-capacity spare host ", hostId);
        return new int[]{hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-saturation-loadstate";
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
