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
 * Strategy: power-efficiency ranked consolidation.
 * Among hosts flagged UNDERLOADED, ranks candidates by watts-per-MIPS at
 * maximum utilisation (max power divided by total MIPS capacity) rather
 * than by disruption. The least energy-efficient host - typically the
 * oldest hardware - is shut down first, prioritising energy goals over
 * migration-count minimisation.
 */
public class planner_v18 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v18";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity worst = null;
        double worstEfficiency = -1.0;

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
            double totalMips = readSpace.getHostTotalMips(host);
            if (totalMips <= 0.0) {
                continue;
            }
            double efficiency = readSpace.getHostMaxPower(host) / totalMips;
            if (efficiency > worstEfficiency) {
                worstEfficiency = efficiency;
                worst = host;
            }
        }

        if (worst == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded host with valid efficiency profile found");
            return new int[0];
        }

        int hostId = readSpace.getId(worst);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] powering down least efficient host " + hostId + " with watts-per-mips " + worstEfficiency);
        return new int[]{hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-power-efficiency-underload";
    }

    @Override
    public String outputSemantic() {
        return "power-down-least-efficient-host";
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
