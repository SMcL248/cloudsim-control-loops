package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        HostEntity chosen = null;
        int fewestVms = Integer.MAX_VALUE;

        int limit = Math.min(diagnosis.length, hosts.size());
        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) {
                continue;
            }
            int vmCount = readSpace.getVmListForHost(host).size();
            if (vmCount < fewestVms) {
                fewestVms = vmCount;
                chosen = host;
            }
        }

        if (chosen == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] no idle host available for consolidation power-down");
            return new int[0];
        }

        int hostId = readSpace.getId(chosen);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] host ", hostId, " selected for power-down, hosted VM count=", fewestVms);
        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-underload-consolidation-candidate";
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
