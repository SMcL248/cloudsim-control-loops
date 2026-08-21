package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v4 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1204;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        int spiking = 0;

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPoweringUp(host)) {
                double headroom = readSpace.getHostMaxPower(host) - readSpace.getHostPower(host);
                result[i] = Math.max(headroom, 0.0);
                spiking++;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] ", spiking, " of ", hosts.size(), " hosts mid power-up, exposed to transient spike");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-powerup_spike_risk_watts";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
