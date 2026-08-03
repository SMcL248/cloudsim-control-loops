package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class monitor_v5 implements Monitor<double[]> {

    private static final String SEMANTIC = "host-status-availability";
    private static final int GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = -1.0;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = 0.0;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = 0.25;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = 0.5;
            } else {
                result[i] = 1.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] host availability status classified for ", hosts.size(), " hosts");
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
