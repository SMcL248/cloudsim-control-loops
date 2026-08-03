package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class monitor_v4 implements Monitor<double[]> {

    private static final String SEMANTIC = "host-power-efficiency";
    private static final int GUID = 1200;
    private static final double MIN_POWER = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                result[i] = 0.0;
                continue;
            }

            double throughput = 0.0;
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                throughput += readSpace.getVmEffectiveThroughput(vm);
            }

            double power = readSpace.getHostPower(host);
            result[i] = throughput / Math.max(power, MIN_POWER);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] power efficiency ratio computed for ", hosts.size(), " hosts");
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
