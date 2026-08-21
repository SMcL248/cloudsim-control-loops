package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 7: Proactive capacity provisioning.
 * Strategy: the inverse of idle shutdown. Read host-level diagnosis as a
 * systemic congestion signal; once the fraction of OVERLOADED hosts crosses
 * a threshold, wake the first available powered-down (and recoverable)
 * host to add capacity ahead of further demand, rather than waiting for
 * a specific host or VM to individually breach.
 */
public class planner_v7 implements Planner<LoadState[], int[]> {

    private static final double PROVISION_THRESHOLD = 0.4;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int considered = Math.min(diagnosis.length, hosts.size());
        if (considered == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ", "No diagnosis data, skipping.");
            return new int[]{-1};
        }

        int overloadedCount = 0;
        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }
        double ratio = (double) overloadedCount / considered;

        if (ratio <= PROVISION_THRESHOLD) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ",
                    "Systemic overload ratio " + ratio + " below provisioning threshold.");
            return new int[]{-1};
        }

        for (HostEntity host : hosts) {
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) && !readSpace.isHostPoweringUp(host)) {
                int hostId = readSpace.getId(host);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ",
                        "Systemic overload ratio " + ratio + " detected, proactively powering up spare host " + hostId);
                return new int[]{hostId};
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] ",
                "Systemic overload detected but no spare powered-down host available.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-overload-systemic";
    }

    @Override
    public String outputSemantic() {
        return "host-powerup-provision";
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
