package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Strategy: zero-risk power saving. Diagnosis is per-host. Powers down an underloaded host only if it
// is already empty of VMs, so the action can never evacuate or strand hosted workloads -- unlike
// powering down an occupied host, which would destroy everything it carries.
public class planner_v9 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || hosts == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] diagnosis/host list mismatch, no-op");
            return new int[0];
        }

        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) continue;

            List<GuestEntity> hosted = readSpace.getVmListForHost(host);
            if (hosted != null && hosted.isEmpty()) {
                int hostId = readSpace.getId(host);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v9] powering down empty underloaded host ", hostId);
                return new int[] { hostId };
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] no empty underloaded host available to power down, no-op");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "host-mips-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerDown";
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
