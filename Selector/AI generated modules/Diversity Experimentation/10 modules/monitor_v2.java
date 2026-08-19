package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: power draw ratio.
// For each host, reports current power consumption as a fraction of that
// host's maximum possible power draw. A powered-down host explicitly reports
// zero, since it is confirmed to draw no power.
public class monitor_v2 implements Monitor<double[]> {

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

            double maxPower = readSpace.getHostMaxPower(host);
            if (maxPower <= 0.0) {
                result[i] = -1.0;
                continue;
            }

            double power = readSpace.getHostPower(host);
            result[i] = power / maxPower;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] computed host power draw ratio for ",
                hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-power-draw-ratio-current-over-max-power";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
