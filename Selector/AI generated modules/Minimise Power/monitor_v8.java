package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v8 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1401;
    private static final double EPS = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        Map<Integer, HostEntity> hostOfVm = new HashMap<Integer, HostEntity>();
        for (HostEntity host : readSpace.getAllHosts()) {
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                hostOfVm.put(readSpace.getId(vm), host);
            }
        }

        Map<Integer, GuestEntity> vmOfCloudlet = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                vmOfCloudlet.put(readSpace.getId(cl), vm);
            }
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        double now = readSpace.getNow();

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = vmOfCloudlet.get(readSpace.getId(cl));

            if (vm == null) {
                result[i] = 0.0;
                continue;
            }

            HostEntity host = hostOfVm.get(readSpace.getId(vm));
            if (host == null) {
                result[i] = 0.0;
                continue;
            }

            double hostTotalMips = readSpace.getHostTotalMips(host);
            if (hostTotalMips <= EPS) {
                result[i] = 0.0;
                continue;
            }

            double util = 1.0 - (readSpace.getHostAvailableMips(host) / hostTotalMips);
            util = Math.max(0.0, Math.min(1.0, util));

            double remainingTime = readSpace.getCloudletEstimatedFinishTime(vm, cl) - now;
            remainingTime = Math.max(remainingTime, 0.0);

            result[i] = readSpace.getHostEnergyEstimate(host, util, util, remainingTime);
        }

        Log.printlnConcat(now, ": [monitor_v8] Projected completion energy for ", cloudlets.size(), " active cloudlets at steady-state host utilisation");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimated_completion_energy_joules";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
