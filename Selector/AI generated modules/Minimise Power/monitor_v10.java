package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v10 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1403;
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

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = vmOfCloudlet.get(readSpace.getId(cl));
            HostEntity host = vm == null ? null : hostOfVm.get(readSpace.getId(vm));

            if (host == null) {
                result[i] = 0.0;
                continue;
            }

            double hostTotalMips = readSpace.getHostTotalMips(host);
            double wattsPerMips = hostTotalMips > EPS ? readSpace.getHostPower(host) / hostTotalMips : 0.0;

            result[i] = readSpace.getRemainingLength(cl) * wattsPerMips;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] Weighted remaining work by static host power-per-MIPS inefficiency for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-host_inefficiency_exposure";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
