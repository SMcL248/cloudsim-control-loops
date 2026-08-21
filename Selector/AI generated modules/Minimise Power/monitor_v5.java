package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v5 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1301;
    private static final double EPS = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        Map<Integer, Double> hostThroughput = new HashMap<Integer, Double>();
        Map<Integer, HostEntity> hostOfVm = new HashMap<Integer, HostEntity>();

        for (HostEntity host : hosts) {
            double total = 0.0;
            List<GuestEntity> hosted = readSpace.getVmListForHost(host);
            for (GuestEntity vm : hosted) {
                total += readSpace.getVmEffectiveThroughput(vm);
                hostOfVm.put(readSpace.getId(vm), host);
            }
            hostThroughput.put(readSpace.getId(host), total);
        }

        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        int unattributed = 0;

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            HostEntity host = hostOfVm.get(readSpace.getId(vm));

            if (host == null) {
                result[i] = 0.0;
                unattributed++;
                continue;
            }

            double totalThroughput = hostThroughput.get(readSpace.getId(host));
            double vmThroughput = readSpace.getVmEffectiveThroughput(vm);
            double hostPower = readSpace.getHostPower(host);

            if (totalThroughput > EPS) {
                result[i] = hostPower * (vmThroughput / totalThroughput);
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] Apportioned host power to ", vms.size(), " VMs by throughput share, ", unattributed, " unplaced");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-proportional_power_attribution_watts";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
