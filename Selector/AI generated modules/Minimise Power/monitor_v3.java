package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v3 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1203;
    private static final double EPS = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];
        int wastefulHosts = 0;

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPoweredDown(host)) {
                result[i] = 0.0;
                continue;
            }

            double activeUtil = 0.0;
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                activeUtil += readSpace.getVmCpuUtil(vm);
            }

            boolean isStalled = readSpace.isHostFailed(host);
            boolean isIdle = activeUtil <= EPS;

            if (isStalled || isIdle) {
                result[i] = readSpace.getHostPower(host);
                wastefulHosts++;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] Flagged ", wastefulHosts, " of ", hosts.size(), " hosts drawing power with no productive work");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-idle_waste_power_watts";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
