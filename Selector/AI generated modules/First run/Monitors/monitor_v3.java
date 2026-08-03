package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class monitor_v3 implements Monitor<double[]> {

    private static final String SEMANTIC = "host-vmcount-active";
    private static final int GUID = 1200;

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

            List<GuestEntity> vmsOnHost = readSpace.getVmListForHost(host);
            int activeCount = 0;

            for (GuestEntity vm : vmsOnHost) {
                if (!readSpace.isVmBeingInstantiated(vm)) {
                    activeCount++;
                }
            }

            result[i] = (double) activeCount;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] active vm counts collected for ", hosts.size(), " hosts");
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
