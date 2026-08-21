package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v1 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1201;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        double total = 0.0;

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double power = readSpace.getHostPower(host);
            result[i] = power;
            total += power;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] Sampled instantaneous power draw across ", hosts.size(), " hosts, fleet total = ", total, " W");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-instant_power_watts";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
