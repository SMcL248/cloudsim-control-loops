package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level power cost per unit of spare MIPS capacity (watts / available MIPS).
// Highlights hosts that are burning power while offering little usable headroom -
// good candidates for consolidation to cut power without hurting throughput.
// Hosts with (near) zero spare capacity are flagged with a large sentinel value,
// since their power cost per unit headroom is effectively unbounded.
public class monitor_v3 implements Monitor<double[]> {

    private static final double EPSILON = 1e-6;
    private static final double NO_HEADROOM_SENTINEL = Double.MAX_VALUE;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double available = readSpace.getHostAvailableMips(host);
            double power = readSpace.getHostPower(host);

            if (available > EPSILON) {
                result[i] = power / available;
            } else {
                result[i] = NO_HEADROOM_SENTINEL;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] ", "computed power-per-headroom for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-powerCostPerSpareMips-ratio";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
