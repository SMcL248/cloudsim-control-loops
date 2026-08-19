package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Host-level monitor. PEs degrade throughput under load rather than
 * hard-rejecting work, so a host can end up hosting guests that, on
 * paper, request more MIPS in aggregate than the host actually
 * provides. This metric sums the provisioned MIPS request of every VM
 * resident on the host and divides by the host's total MIPS capacity.
 * A value above 1.0 means the host is structurally overcommitted: its
 * guests will compete for cycles even before accounting for their
 * real-time utilisation. This is a static, demand-side signal, distinct
 * from live headroom readings which only reflect the current instant.
 */
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            double totalMips = readSpace.getHostTotalMips(host);
            double requestedSum = 0.0;

            List<GuestEntity> vms = readSpace.getVmListForHost(host);
            if (vms != null) {
                for (GuestEntity vm : vms) {
                    requestedSum += readSpace.getVmRequestedMips(vm);
                }
            }

            result[i] = totalMips > 0 ? requestedSum / totalMips : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] ", "computed soft-overcommit pressure for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-soft-overcommit-pressure-requested-mips-over-capacity";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
