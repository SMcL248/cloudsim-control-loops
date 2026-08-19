package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: static absolute-threshold classifier.
// Fixed domain-informed cutoffs on host CPU utilisation. Simplest possible
// baseline: no adaptivity, no history. Also folds in a hard override -
// a failed host is always reported OVERLOADED regardless of its utilisation
// figure, since it cannot be trusted to safely absorb further work.
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v1";

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpu-utilization-ratio";
    private static final String OUTPUT_SEMANTIC = "host-load-static-threshold-classification";

    private static final double LOW_THRESHOLD = 0.25;
    private static final double HIGH_THRESHOLD = 0.80;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        for (int i = 0; i < n; i++) {
            double util = metrics[i];

            boolean failed = false;
            if (hosts != null && i < hosts.size()) {
                failed = readSpace.isHostFailed(hosts.get(i));
            }

            if (failed) {
                result[i] = LoadState.OVERLOADED;
                continue;
            }

            if (Double.isNaN(util)) {
                result[i] = LoadState.BALANCED;
            } else if (util >= HIGH_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
            } else if (util <= LOW_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] classified ", n,
                " hosts using static thresholds low=", LOW_THRESHOLD, " high=", HIGH_THRESHOLD);

        return result;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public String outputSemantic() {
        return OUTPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
