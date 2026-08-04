package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: reports a continuous availability-risk score per host, combining
// failure, permanent-death, and power-up transition state into a single number.
// Supports the availability goal by flagging hosts whose capacity cannot currently be relied on.
// Scoring: 1.0 = permanently dead (capacity gone for good), 0.75 = currently failed
// (paused, may recover), 0.25 = powering up (capacity not yet usable), 0.0 = healthy.
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                metrics[i] = 1.0;
            } else if (readSpace.isHostFailed(host)) {
                metrics[i] = 0.75;
            } else if (readSpace.isHostPoweringUp(host)) {
                metrics[i] = 0.25;
            } else {
                metrics[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] computed availability risk score for ", hosts.size(), " hosts");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "host-availability-risk-score-failure-death-powerup-per-host";
    }

    @Override
    public int outputGuid() {
        return 1204;
    }

}
