package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * planner_v1
 *
 * Strategy: "Safe consolidation power-down"
 * Host-level diagnosis. Among hosts classified UNDERLOADED, selects one that
 * currently hosts zero VMs (so powering it down destroys no workload) and is
 * not already powered down/powering up/failed/dead, preferring the host
 * with the highest current power draw (biggest immediate energy saving).
 * Emits requestHostPowerDown{hostId}, or an empty array if no safe
 * candidate exists.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity bestCandidate = null;
        double bestPower = -1.0;

        int limit = Math.min(diagnosis.length, hosts.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }

            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (!readSpace.getVmListForHost(host).isEmpty()) {
                // Powering down would destroy hosted VMs and their workloads; skip.
                continue;
            }

            double power = readSpace.getHostPower(host);
            if (power > bestPower) {
                bestPower = power;
                bestCandidate = host;
            }
        }

        if (bestCandidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "no idle underloaded host found to power down");
            return new int[0];
        }

        int hostId = readSpace.getId(bestCandidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ",
                "powering down idle underloaded host " + hostId + " drawing " + bestPower + " W");
        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "host-power-down";
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
