package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Variant 3: stability-weighted available MIPS headroom.
// Headroom on a host that is failed, permanently dead, powered down, or
// mid power-up is not real capacity a planner should act on - it is zeroed
// out here so downstream modules are not steered toward capacity that will
// not materialise, protecting throughput instead of maximising raw numbers.
public class monitor_v3 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v3";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        int unstableCount = 0;

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            boolean unstable = readSpace.isHostFailed(host)
                    || readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostPoweredDown(host)
                    || readSpace.isHostPoweringUp(host);

            if (unstable) {
                result[i] = 0.0;
                unstableCount++;
            } else {
                result[i] = readSpace.getHostAvailableMips(host);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "stability-weighted headroom computed, ", unstableCount, " hosts zeroed as unstable");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-stability-weighted-mips-headroom";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
