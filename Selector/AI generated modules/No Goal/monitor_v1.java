package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

// Strategy: raw CPU load ratio per host = fraction of total MIPS currently
// in use. The baseline resource-pressure signal at host level.
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);
            double availableMips = readSpace.getHostAvailableMips(host);

            if (totalMips <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = (totalMips - availableMips) / totalMips;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] host CPU load ratio computed for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-cpuLoadRatio-fractionMipsInUse";
    }

    @Override
    public int outputGuid() {
        return 1201;
    }
}
