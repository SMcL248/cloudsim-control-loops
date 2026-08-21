package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v9 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1402;

    @Override
    public double[] observe(ReadSpace readSpace) {
        Map<Integer, HostEntity> hostOfVm = new HashMap<Integer, HostEntity>();
        for (HostEntity host : readSpace.getAllHosts()) {
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                hostOfVm.put(readSpace.getId(vm), host);
            }
        }

        Map<Integer, GuestEntity> vmOfCloudlet = new HashMap<Integer, GuestEntity>();
        Map<Integer, Long> hostRemainingLength = new HashMap<Integer, Long>();

        for (GuestEntity vm : readSpace.getVmList()) {
            HostEntity host = hostOfVm.get(readSpace.getId(vm));
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                vmOfCloudlet.put(readSpace.getId(cl), vm);
                if (host != null) {
                    int hostId = readSpace.getId(host);
                    long prior = hostRemainingLength.containsKey(hostId) ? hostRemainingLength.get(hostId) : 0L;
                    hostRemainingLength.put(hostId, prior + readSpace.getRemainingLength(cl));
                }
            }
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = vmOfCloudlet.get(readSpace.getId(cl));
            HostEntity host = vm == null ? null : hostOfVm.get(readSpace.getId(vm));

            if (host == null) {
                result[i] = 0.0;
                continue;
            }

            long totalRemaining = hostRemainingLength.containsKey(readSpace.getId(host))
                    ? hostRemainingLength.get(readSpace.getId(host)) : 0L;

            if (totalRemaining <= 0) {
                result[i] = 0.0;
                continue;
            }

            double share = (double) readSpace.getRemainingLength(cl) / (double) totalRemaining;
            result[i] = readSpace.getHostPower(host) * share;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] Split current host power draw across ", cloudlets.size(), " active cloudlets by remaining-work share");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-power_density_per_remaining_work";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
