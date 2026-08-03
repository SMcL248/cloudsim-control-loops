package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v1 - Host-level CPU utilisation classifier (z-score band).
 *
 * Level        : Host (level 2)
 * Metric       : Per-host CPU utilisation ratio, range approx [0, 1+].
 * Threshold    : Dynamic - classifies relative to the mean and standard
 *                deviation observed across the current snapshot, rather
 *                than fixed cut-offs. Hosts more than one standard
 *                deviation above the mean are OVERLOADED; more than one
 *                standard deviation below are UNDERLOADED.
 * Failure rule : A failed host is always reported OVERLOADED, since it
 *                cannot make progress on its assigned workload -
 *                supports the throughput goal by flagging it for
 *                migration/evacuation.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = (n > 0) ? sum / n : 0.0;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = (n > 0) ? Math.sqrt(sqDiffSum / n) : 0.0;

        double upperBound = mean + stdDev;
        double lowerBound = mean - stdDev;

        for (int i = 0; i < n; i++) {
            boolean failed = (i < hosts.size()) && readSpace.isHostFailed(hosts.get(i));

            if (failed) {
                states[i] = LoadState.OVERLOADED;
            } else if (metrics[i] > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (metrics[i] < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v1] classified ", n,
                " hosts, mean=", mean, " stddev=", stdDev);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-util-ratio";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-zscore";
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
