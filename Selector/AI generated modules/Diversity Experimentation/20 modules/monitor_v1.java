package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - instantaneous CPU utilization ratio per host.
public class monitor_v1 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double availableMips = readSpace.getHostAvailableMips(host);
            double usedMips = totalMips - availableMips;
            result[i] = (totalMips > 0.0) ? (usedMips / totalMips) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] computed host-cpu-utilization-ratio for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-cpu-utilization-ratio: instantaneous fraction of a host's total MIPS capacity currently in use, "
                + "computed as (totalMips - availableMips) / totalMips, range 0.0-1.0. Index i corresponds to "
                + "getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
