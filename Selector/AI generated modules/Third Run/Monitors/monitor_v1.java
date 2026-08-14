package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level instantaneous CPU utilization: fraction of total MIPS currently in use.
// Simple load signal for throughput-oriented decisions (e.g. finding overloaded hosts).
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double total = readSpace.getHostTotalMips(host);
            double available = readSpace.getHostAvailableMips(host);
            result[i] = (total > 0.0) ? (1.0 - (available / total)) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] ", "computed CPU utilization for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-cpuUtilization-instantaneous";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
