package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: reports spare RAM capacity per host.
// Supports the availability goal of preserving host capacity for new or migrating VMs
// (RAM is frequently the binding constraint on host consolidation, distinct from MIPS).
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            metrics[i] = readSpace.getHostAvailableRam(host);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] computed available RAM for ", hosts.size(), " hosts");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "host-available-ram-spare-memory-capacity-per-host";
    }

    @Override
    public int outputGuid() {
        return 1201;
    }

}
