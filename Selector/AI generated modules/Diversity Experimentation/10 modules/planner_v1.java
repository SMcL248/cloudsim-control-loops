package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: Safe idle consolidation.
// Powers down an UNDERLOADED host only when it is already carrying zero guests,
// so the action never destroys live workload. Among all such empty, safely
// power-downable hosts, the weakest one (lowest total MIPS) is chosen first,
// preserving stronger hosts as standby capacity for future demand spikes.
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "evaluating hosts for safe idle power-down");

        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity weakestCandidate = null;
        double weakestMips = Double.MAX_VALUE;

        int limit = Math.min(diagnosis.length, hosts.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }

            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }

            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            if (!guests.isEmpty()) {
                continue;
            }

            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips < weakestMips) {
                weakestMips = totalMips;
                weakestCandidate = host;
            }
        }

        if (weakestCandidate == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "no empty underloaded host available to power down");
            return null;
        }

        int hostId = readSpace.getId(weakestCandidate);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ", "selected empty host for power-down hostId=" + hostId);

        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-aggregateutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "host-powerdown-idle-consolidation";
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
