package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level planner. diagnosis[i] is the load state of readSpace.getAllHosts().get(i).
// Goal: minimise total raw energy consumed.
// Strategy: an UNDERLOADED host that currently hosts no guests is drawing
// idle power for no return. Power it down. Guarded against hosts that are
// failed, permanently dead, already powered down, or mid power-up.
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v5";
    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3010;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();

        if (diagnosis == null || diagnosis.length != hosts.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/host size mismatch, no-op");
            return new int[]{-1};
        }

        for (int i = 0; i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) continue;
            if (!readSpace.getVmListForHost(host).isEmpty()) continue;

            int hostId = readSpace.getId(host);
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan power down idle host ", hostId);
            return new int[]{hostId};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no idle powerable-down host found, no-op");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "host-mips-idle-underload";
    }

    @Override
    public String outputSemantic() {
        return "requesthostpowerdown";
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
