package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: reports spare compute capacity (available MIPS) per host.
// Supports the availability goal of preserving host capacity for new or migrating VMs.
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            metrics[i] = readSpace.getHostAvailableMips(host);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] computed available MIPS for ", hosts.size(), " hosts");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "host-available-mips-spare-compute-capacity-per-host";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }

}
