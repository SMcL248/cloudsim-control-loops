package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

public class monitor_v1 implements Monitor<double[]> {

    private static final String SEMANTIC = "host-power-current";
    private static final int GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                result[i] = 0.0;
            } else {
                result[i] = readSpace.getHostPower(host);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] host power draw sampled for ", hosts.size(), " hosts");
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
