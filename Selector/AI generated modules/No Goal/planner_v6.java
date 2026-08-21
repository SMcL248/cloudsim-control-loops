package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 6: Energy-aware idle-host shutdown.
 * Strategy: read host-level diagnosis. An UNDERLOADED host is only powered
 * down once confirmed to be hosting zero guests - avoiding the destructive
 * side-effect of requestHostPowerDown (which would otherwise destroy any
 * resident VMs and their workloads). Pure consolidation-for-energy policy,
 * indifferent to any other host's state.
 */
public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int considered = Math.min(diagnosis.length, hosts.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.getVmListForHost(host).isEmpty()) {
                continue;
            }

            int hostId = readSpace.getId(host);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ",
                    "Host " + hostId + " underloaded and empty of guests, requesting power-down to conserve energy.");
            return new int[]{hostId};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] ",
                "No idle underloaded host eligible for power-down.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-underload-idle";
    }

    @Override
    public String outputSemantic() {
        return "host-powerdown-consolidate";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3010;
    }
}
