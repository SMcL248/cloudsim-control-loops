package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

// Variant 2: host-level health/lifecycle status code.
// Distinct from monitor_v1's continuous utilisation ratio: this reports
// discrete lifecycle state (healthy / off / powering up / failed-recoverable
// / permanently dead), which a utilisation number alone cannot disambiguate
// (e.g. a host reading 0.0 utilisation could be idle-and-healthy or
// failed-and-empty; those require very different responses downstream).
// Shares GUID 1200 with monitor_v1 intentionally: both are host-level (level 2)
// metrics, but GUID equality is purely mechanical here -- the semantic string
// is what actually distinguishes "utilisation ratio" from "status code" for
// any consumer, so consumers must match on outputSemantic(), not just guid.
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] status = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostPermanentlyDead(host)) {
                status[i] = 3.0;
            } else if (readSpace.isHostFailed(host)) {
                status[i] = 2.0;
            } else if (readSpace.isHostPoweringUp(host)) {
                status[i] = 1.0;
            } else if (readSpace.isHostPoweredDown(host)) {
                status[i] = 0.5;
            } else {
                status[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] host health status codes computed for ", hosts.size(), " hosts (index-aligned with getAllHosts).");
        return status;
    }

    @Override
    public String outputSemantic() {
        return "2-hostHealthStatusCode-0healthyRunning_0p5poweredOff_1poweringUp_2failedRecoverable_3permanentlyDead";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }

}
