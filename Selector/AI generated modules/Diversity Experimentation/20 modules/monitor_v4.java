package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - ordinal encoding of host lifecycle/health state.
public class monitor_v4 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = 0.0;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = 0.25;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = 0.5;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = 0.75;
            } else {
                result[i] = 1.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] computed host-operational-health-score for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-operational-health-score: ordinal encoding of host lifecycle state - 0.0 = permanently dead, "
                + "0.25 = failed but potentially recoverable, 0.5 = powered down, 0.75 = powering up (transient "
                + "power spike), 1.0 = healthy and fully operational. Index i corresponds to getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
