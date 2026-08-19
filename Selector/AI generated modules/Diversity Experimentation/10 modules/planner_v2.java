package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: Preemptive headroom provisioning, capability-first.
// When any host in the diagnosis is OVERLOADED, this planner does not touch
// workload placement at all. Instead it treats infrastructure capacity itself
// as the lever: it wakes the most capable currently powered-down host (highest
// total MIPS) so that strong standby capacity is ready before any migration or
// scaling decision is made elsewhere in the control loop.
public class planner_v2 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "checking for overload signal requiring reserve capacity");

        boolean overloadDetected = false;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadDetected = true;
                break;
            }
        }

        if (!overloadDetected) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "no overload signal, reserve capacity not needed");
            return null;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity strongestReserve = null;
        double strongestMips = -1;

        for (HostEntity host : hosts) {

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (!readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }

            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips > strongestMips) {
                strongestMips = totalMips;
                strongestReserve = host;
            }
        }

        if (strongestReserve == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "no eligible powered-down host to activate");
            return null;
        }

        int hostId = readSpace.getId(strongestReserve);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v2] ", "activating reserve hostId=" + hostId);

        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-aggregateutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "host-powerup-headroom-reserve";
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
