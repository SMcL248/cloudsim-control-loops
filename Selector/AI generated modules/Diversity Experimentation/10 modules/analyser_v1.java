package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/*
 * Variant: analyser_v1
 * Level: HOST
 * Metric: CPU utilisation fraction
 * Strategy: fixed absolute thresholds, with an explicit override for hosts
 * currently in a failed state. A failed host has its workload paused, so a
 * stale high-utilisation reading does not reflect real contention and must
 * not be reported as OVERLOADED.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_THRESHOLD = 0.80;
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        LoadState[] states = new LoadState[metrics.length];
        int overloaded = 0, underloaded = 0, balanced = 0;

        for (int i = 0; i < metrics.length; i++) {
            HostEntity host = hosts.get(i);

            if (readSpace.isHostFailed(host)) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] host ", readSpace.getId(host), " is failed, workload paused -> UNDERLOADED");
                continue;
            }

            double util = metrics[i];
            if (util >= OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (util <= UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] fixed-threshold host classification complete: ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-fixed-threshold";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}
