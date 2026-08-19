package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/*
 * Variant: analyser_v6
 * Level: HOST
 * Metric: CPU utilisation fraction
 * Strategy: purely local/structural classification - no comparison against
 * other hosts in the batch at all. Each host's overload/underload band
 * width is derived from its own PE count: hosts with more PEs average out
 * transient utilisation spikes across more processing elements, so their
 * utilisation reading is statistically smoother and can be given a tighter
 * band around the 0.5 midpoint before being flagged.
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final double CENTER = 0.5;
    private static final double BASE_RADIUS = 0.30;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        LoadState[] states = new LoadState[metrics.length];
        int overloaded = 0, underloaded = 0, balanced = 0;

        for (int i = 0; i < metrics.length; i++) {
            HostEntity host = hosts.get(i);
            int peCount = readSpace.getHostPeCount(host);
            double radius = peCount > 0 ? BASE_RADIUS / Math.sqrt(peCount) : BASE_RADIUS;

            double util = metrics[i];
            LoadState state;
            if (util > CENTER + radius) {
                state = LoadState.OVERLOADED;
            } else if (util < CENTER - radius) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            states[i] = state;
            switch (state) {
                case OVERLOADED: overloaded++; break;
                case UNDERLOADED: underloaded++; break;
                default: balanced++; break;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] PE-count-relative band classification: ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-pe-relative-band";
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
