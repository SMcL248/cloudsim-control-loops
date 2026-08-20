package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - RAM allocation pressure per host.
public class monitor_v3 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalRam = readSpace.getHostTotalRam(host);
            double availableRam = readSpace.getHostAvailableRam(host);
            result[i] = (totalRam > 0.0) ? ((totalRam - availableRam) / totalRam) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] computed host-ram-pressure-ratio for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-ram-pressure-ratio: fraction of a host's total RAM currently allocated to guests, computed as "
                + "1 - (availableRam / totalRam), range 0.0-1.0. Index i corresponds to getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
