package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

// VM level - this VM's effective throughput as a share of its hosting host's total MIPS capacity.
public class monitor_v11 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            HostEntity ownerHost = findOwningHost(readSpace, hosts, vm);

            if (ownerHost == null) {
                result[i] = 0.0;
            } else {
                double throughput = readSpace.getVmEffectiveThroughput(vm);
                double hostTotalMips = readSpace.getHostTotalMips(ownerHost);
                result[i] = (hostTotalMips > 0.0) ? (throughput / hostTotalMips) : 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v11] computed vm-host-throughput-share for ", vms.size(), " vms.");

        return result;
    }

    private HostEntity findOwningHost(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : hosts) {
            for (GuestEntity candidate : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(candidate) == vmId) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String outputSemantic() {
        return "vm-host-throughput-share: this VM's effective throughput (MIPS) as a fraction of its hosting "
                + "host's total MIPS capacity, indicating how large a slice of shared host capacity this single VM "
                + "currently commands. The owning host is resolved by matching the VM's id against each host's "
                + "getVmListForHost() membership. Value is 0.0 if the VM's host cannot be resolved (for example, "
                + "not yet instantiated). Index i corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
