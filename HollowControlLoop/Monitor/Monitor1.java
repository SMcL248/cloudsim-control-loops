package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Monitor1 implements Monitor {

    @Override
    public Map<GuestEntity, Map<String, Double>> observe(WorldState worldState) {

        double now = worldState.now();

        Log.printlnConcat(now, ": Observing...");

        Map<GuestEntity, Map<String, Double>> metrics = new HashMap<>();

        for (GuestEntity vm : worldState.guests()) {
            long remainingWork = 0;
            for (Cloudlet cloudlet : vm.getCloudletScheduler().getCloudletExecList()) {
                remainingWork += cloudlet.getRemainingCloudletLength();
            }

            double currentETC = remainingWork / vm.getMips();
            int numCloudlets = vm.getCloudletScheduler().getCloudletExecList().size();
            Log.printlnConcat("VM #", vm.getId(), "| MIPS: ", vm.getMips(), "| Outstanding Work: ", remainingWork, " | Current ETC: ", currentETC, " | Number of Cloudlets: ", numCloudlets);
            metrics.put(vm, Map.of("etc", currentETC));
        }

        return metrics;
    }

    @Override
    public Set<String> providedMetrics() {

        return Set.of("etc");

    }
}
