package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - current power draw normalized against the host's max possible power draw.
public class monitor_v2 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double power = readSpace.getHostPower(host);
            double maxPower = readSpace.getHostMaxPower(host);
            result[i] = (maxPower > 0.0) ? (power / maxPower) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] computed host-power-stress-ratio for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-power-stress-ratio: current host power draw (Watts) expressed as a fraction of the host's "
                + "maximum power draw at 100% utilization, range 0.0-1.0. Index i corresponds to getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
