package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level operational status code, for resilience-aware planning under host failure:
//   0 = healthy
//   1 = temporarily failed (workload paused, may be repaired or later evacuated)
//   2 = permanently dead (workload must be evacuated, no repair possible)
//   3 = powered down
//   4 = powering up
public class monitor_v4 implements Monitor<double[]> {

    private static final double HEALTHY = 0.0;
    private static final double TEMP_FAILED = 1.0;
    private static final double PERMANENTLY_DEAD = 2.0;
    private static final double POWERED_DOWN = 3.0;
    private static final double POWERING_UP = 4.0;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = PERMANENTLY_DEAD;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = TEMP_FAILED;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = POWERED_DOWN;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = POWERING_UP;
            } else {
                result[i] = HEALTHY;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] ", "computed operational status for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-operationalStatus-code";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
