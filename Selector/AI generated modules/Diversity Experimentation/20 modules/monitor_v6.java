package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Host level - forward-looking marginal energy cost of saturating this host to 100% utilization.
public class monitor_v6 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double availableMips = readSpace.getHostAvailableMips(host);
            double currentUtil = (totalMips > 0.0) ? ((totalMips - availableMips) / totalMips) : 0.0;
            result[i] = readSpace.getHostEnergyEstimate(host, currentUtil, 1.0, 1.0);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] computed host-saturation-energy-cost for ", hosts.size(), " hosts.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-saturation-energy-cost: estimated marginal energy required to move this host from its current "
                + "utilization to 100% utilization over one simulated time unit, via getHostEnergyEstimate(host, "
                + "currentUtil, 1.0, 1.0). A forward-looking stress indicator distinct from instantaneous power "
                + "draw. Index i corresponds to getAllHosts().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
