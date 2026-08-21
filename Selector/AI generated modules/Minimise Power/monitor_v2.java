package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v2 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1202;
    private static final double EPS = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        int starved = 0;

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double deliveredMips = 0.0;

            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                deliveredMips += readSpace.getVmEffectiveThroughput(vm);
            }

            double power = readSpace.getHostPower(host);
            if (power > EPS) {
                result[i] = deliveredMips / power;
            } else {
                result[i] = 0.0;
                starved++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] Computed delivered-MIPS-per-Watt efficiency for ", hosts.size(), " hosts, ", starved, " had negligible power draw");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-mips_per_watt_efficiency";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
