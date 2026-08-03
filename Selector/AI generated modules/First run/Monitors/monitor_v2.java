package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class monitor_v2 implements Monitor<double[]> {

    private static final String SEMANTIC = "host-util-mips";
    private static final int GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);

            if (readSpace.isHostFailed(host) || totalMips <= 0.0) {
                result[i] = 0.0;
            } else {
                double availableMips = readSpace.getHostAvailableMips(host);
                double usedMips = totalMips - availableMips;
                result[i] = usedMips / totalMips;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] host mips utilisation computed for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int outputGuid() {
        return GUID;
    }
}
