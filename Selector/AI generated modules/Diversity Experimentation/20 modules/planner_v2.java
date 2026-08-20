package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

/**
 * Strategy: least-disruptive consolidation.
 * Among hosts flagged UNDERLOADED, power down the one hosting the fewest
 * guest VMs, minimizing the number of VMs that must be evacuated as a
 * side effect of the shutdown.
 */
public class planner_v2 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v2";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity best = null;
        int bestVmCount = Integer.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
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
            int vmCount = readSpace.getVmListForHost(host).size();
            if (vmCount < bestVmCount) {
                bestVmCount = vmCount;
                best = host;
            }
        }

        if (best == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded host eligible for power-down");
            return new int[0];
        }

        int hostId = readSpace.getId(best);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] powering down host " + hostId + " carrying only " + bestVmCount + " vms");
        return new int[]{hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-consolidation-candidate";
    }

    @Override
    public String outputSemantic() {
        return "power-down-least-disruptive-host";
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
