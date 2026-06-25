package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Monitor1 implements Monitor<Map<GuestEntity, Map<String, Double>>> {

    @Override
    public Map<GuestEntity, Map<String, Double>> observe(ReadSpace readSpace) {

        double now = readSpace.getNow();

        Log.printlnConcat(now, ": Observing...");

        Map<GuestEntity, Map<String, Double>> metrics = new HashMap<>();

        for (GuestEntity vm : readSpace.getVmList()) {
            long remainingWork = 0;
            for (Cloudlet cloudlet : vm.getCloudletScheduler().getCloudletExecList()) {
                remainingWork += cloudlet.getRemainingCloudletLength();
            }

            double currentETC = remainingWork / vm.getMips();
            int numCloudlets = vm.getCloudletScheduler().getCloudletExecList().size();
            Log.printlnConcat(now, ": VM #", vm.getId(), "| MIPS: ", vm.getMips(), "| Outstanding Work: ", remainingWork, " | Current ETC: ", currentETC, " | Number of Cloudlets: ", numCloudlets);
            metrics.put(vm, Map.of("etc", currentETC));

        }

        return metrics;
    }

    @Override
    public String outputGuid() {

        return "etc";

    }
}
