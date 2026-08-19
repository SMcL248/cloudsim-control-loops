package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Host-level monitor: discrete health/state code.
// Encodes each host's operational state as an integer-valued double so
// downstream modules can branch on failure/power state without needing
// direct access to the boolean predicate calls themselves:
//   0 = healthy, 1 = transiently failed (recoverable), 2 = permanently dead,
//   3 = powered down, 4 = powering up.
// Checked in descending order of severity so a host cannot be miscategorised
// as merely "powering up" while it is actually permanently dead.
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                result[i] = 2.0;
            } else if (readSpace.isHostFailed(host)) {
                result[i] = 1.0;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = 3.0;
            } else if (readSpace.isHostPoweringUp(host)) {
                result[i] = 4.0;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] computed host health state code for ",
                hosts.size(), " hosts");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-health-state-code-0healthy-1failed-2dead-3off-4poweringup";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
