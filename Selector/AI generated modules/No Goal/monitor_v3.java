package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: RAM pressure ratio per host. Memory is provisioned as a hard
// pool (unlike MIPS, which degrades gracefully), so tracking it separately
// from CPU load exposes a distinct failure mode: hosts that are CPU-light
// but memory-constrained.
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalRam = readSpace.getHostTotalRam(host);
            double availableRam = readSpace.getHostAvailableRam(host);

            if (totalRam <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = (totalRam - availableRam) / totalRam;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] host RAM pressure ratio computed for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-ramPressureRatio-usedRamOverTotalRam";
    }

    @Override
    public int outputGuid() {
        return 1203;
    }
}
