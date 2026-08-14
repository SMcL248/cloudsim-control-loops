package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Strategy: throughput-led elasticity, mirror image of a consolidation policy. Diagnosis is per-host.
// When overload is widespread across the fleet, proactively wakes a powered-down host ahead of a
// migration/creation storm, rather than waiting for overload to force the issue reactively.
public class planner_v10 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || hosts == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] diagnosis/host list mismatch, no-op");
            return new int[0];
        }

        int overloadedCount = 0;
        for (LoadState s : diagnosis) {
            if (s == LoadState.OVERLOADED) overloadedCount++;
        }
        double overloadFraction = (double) overloadedCount / diagnosis.length;

        if (overloadFraction <= 0.3) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] overload fraction ", overloadFraction, " below power-up threshold, no-op");
            return new int[0];
        }

        for (HostEntity host : hosts) {
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host) && !readSpace.isHostPoweringUp(host)) {
                int hostId = readSpace.getId(host);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v10] overload fraction ", overloadFraction, " -- powering up host ", hostId, " for extra capacity");
                return new int[] { hostId };
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] no powered-down host available to wake, no-op");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "host-mips-load-state";
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
