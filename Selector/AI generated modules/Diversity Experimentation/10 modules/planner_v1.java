package org.cloudbus.cloudsim.examples;

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

// Idle-Host Consolidation Planner.
// Scans host-level LoadState[] for UNDERLOADED hosts that currently host zero
// VMs, and are otherwise healthy and already powered on. Among the eligible
// idle hosts, picks the one with the greatest max power draw so that powering
// it down yields the largest energy saving.
public class planner_v1 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3010;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        HostEntity target = null;
        double bestSavings = -1.0;

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
                continue;
            }
            double maxPower = readSpace.getHostMaxPower(host);
            if (maxPower > bestSavings) {
                bestSavings = maxPower;
                target = host;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] no idle host available to power down");
            return new int[0];
        }

        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] powering down idle host ", hostId);
        return new int[] { hostId };
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerDown";
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
