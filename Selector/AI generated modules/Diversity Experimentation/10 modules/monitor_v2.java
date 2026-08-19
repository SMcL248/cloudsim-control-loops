package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Host-level monitor. Encodes each host's operational state as a small
 * ordinal scale rather than a continuous resource reading:
 *   -1 = powered down (intentionally off, no risk)
 *    0 = healthy and running normally
 *    1 = powering up (transient power-spike window)
 *    2 = failed but potentially recoverable (workload paused)
 *    3 = permanently dead (unrecoverable, evacuation risk)
 * This gives downstream modules a cheap, discrete read of host lifecycle
 * state without having to re-derive it from several boolean flags each
 * time.
 */
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = 3.0;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = 2.0;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = 1.0;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = -1.0;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] ", "encoded health state for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-operational-health-state-ordinal-encoding";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
