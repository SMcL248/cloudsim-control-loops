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

// Variant 1: host-level CPU utilisation ratio.
// Reports how much of each host's total MIPS capacity is currently in use.
// Hosts that are off or failed are not meaningfully "utilised" in the
// scheduling sense, so they are flagged with the -1.0 sentinel instead of
// a misleading 0.0, keeping "idle but healthy" distinct from "unusable".
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] util = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = readSpace.getHostTotalMips(host);

            if (readSpace.isHostFailed(host) || readSpace.isHostPoweredDown(host) || totalMips <= 0.0) {
                util[i] = -1.0;
            } else {
                double availableMips = readSpace.getHostAvailableMips(host);
                double u = 1.0 - (availableMips / totalMips);
                if (u < 0.0) {
                    u = 0.0;
                }
                if (u > 1.0) {
                    u = 1.0;
                }
                util[i] = u;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] host cpu utilisation ratio computed for ", hosts.size(), " hosts (index-aligned with getAllHosts).");
        return util;
    }

    @Override
    public String outputSemantic() {
        return "2-hostCpuUtilRatio-fractionOfTotalMipsCurrentlyInUse_negOneIfOffOrFailed";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }

}
