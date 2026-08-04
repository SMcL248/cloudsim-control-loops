package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: reports current power draw (Watts) per host.
// Supports the energy goal of minimising power (total energy / makespan).
// Permanently dead hosts draw no power and are reported as zero.
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostPermanentlyDead(host)) {
                metrics[i] = 0.0;
            } else {
                metrics[i] = readSpace.getHostPower(host);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] computed current power draw for ", hosts.size(), " hosts");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "host-power-draw-watts-current-per-host";
    }

    @Override
    public int outputGuid() {
        return 1202;
    }

}
