package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Variant 2: MIPS-per-watt headroom per host.
// Weights spare capacity by current power draw, so scale-out decisions
// favour hosts that deliver more throughput per unit of energy spent -
// distinct from v1's raw capacity view because two hosts with identical
// headroom can have very different energy cost to exploit it.
public class monitor_v2 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v2";
    private static final double EPSILON = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPoweredDown(host)) {
                result[i] = 0.0;
                continue;
            }

            double availableMips = readSpace.getHostAvailableMips(host);
            double currentDraw = readSpace.getHostPower(host);
            result[i] = availableMips / Math.max(currentDraw, EPSILON);
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "MIPS-per-watt headroom computed for ", hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-mips-per-watt-headroom";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
