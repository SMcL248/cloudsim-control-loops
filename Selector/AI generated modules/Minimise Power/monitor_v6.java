package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class monitor_v6 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1302;
    private static final double EPS = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        Map<Integer, HostEntity> hostOfVm = new HashMap<Integer, HostEntity>();
        for (HostEntity host : readSpace.getAllHosts()) {
            for (GuestEntity vm : readSpace.getVmListForHost(host)) {
                hostOfVm.put(readSpace.getId(vm), host);
            }
        }

        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
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

            double availableMips = readSpace.getHostAvailableMips(host);
            double currentUtil = 1.0 - (availableMips / hostTotalMips);
            currentUtil = Math.max(0.0, Math.min(1.0, currentUtil));

            double vmMips = readSpace.getVmRequestedMips(vm);
            double vmShare = vmMips / hostTotalMips;
            double counterfactualUtil = Math.max(currentUtil - vmShare, 0.0);

            double marginalPower = readSpace.getHostPowerAtUtil(host, currentUtil)
                    - readSpace.getHostPowerAtUtil(host, counterfactualUtil);

            result[i] = vmMips > EPS ? marginalPower / vmMips : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] Estimated marginal watts-per-MIPS for ", vms.size(), " VMs via counterfactual host utilisation");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-marginal_power_per_mips";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
