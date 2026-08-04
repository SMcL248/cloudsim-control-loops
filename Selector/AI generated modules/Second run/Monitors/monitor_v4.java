package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: reports MIPS utilization fraction (used capacity / total capacity) per host.
// Supports the energy goal of minimising raw energy consumed, since utilization is the primary
// driver of consolidation and power-down decisions.
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double total = readSpace.getHostTotalMips(host);
            double available = readSpace.getHostAvailableMips(host);

            if (total > 0.0) {
                metrics[i] = (total - available) / total;
            } else {
                metrics[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] computed MIPS utilization fraction for ", hosts.size(), " hosts");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "host-mips-utilization-fraction-used-over-total-per-host";
    }

    @Override
    public int outputGuid() {
        return 1203;
    }

}
