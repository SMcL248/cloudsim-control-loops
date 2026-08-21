package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// Variant 1: raw available MIPS headroom per host.
// Simplest possible spare-capacity signal - identifies which hosts have
// unused processing capacity that could absorb more work to lift throughput.
public class monitor_v1 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v1";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            result[i] = readSpace.getHostAvailableMips(host);
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "raw available MIPS headroom computed for ", hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-available-mips-headroom";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
