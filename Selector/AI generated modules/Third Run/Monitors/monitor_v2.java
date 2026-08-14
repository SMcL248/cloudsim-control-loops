package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Host-level current power draw in watts. Direct signal for power-minimisation goals,
// e.g. identifying consolidation or power-down candidates.
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            result[i] = readSpace.getHostPower(host);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] ", "computed power draw for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-powerDraw-watts";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
