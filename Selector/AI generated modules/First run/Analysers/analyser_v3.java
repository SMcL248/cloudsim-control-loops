package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v3 - Host-level available RAM ratio classifier (fixed bands).
 *
 * Level        : Host (level 2)
 * Metric       : Per-host available RAM as a fraction of total RAM,
 *                range [0, 1].
 * Threshold    : Fixed - below 0.15 free is OVERLOADED (little headroom
 *                to accept further guests), above 0.85 free is
 *                UNDERLOADED (mostly empty, consolidation candidate).
 * PE rule      : A host with no free processing elements is always
 *                OVERLOADED regardless of the RAM reading, since it
 *                cannot host further compute-bound workload.
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_FREE_RAM  = 0.15;
    private static final double UNDERLOAD_FREE_RAM = 0.85;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        for (int i = 0; i < n; i++) {
            boolean noFreePe = (i < hosts.size()) && !readSpace.hostHasFreePe(hosts.get(i));
            double freeRamRatio = metrics[i];

            if (noFreePe || freeRamRatio < OVERLOAD_FREE_RAM) {
                states[i] = LoadState.OVERLOADED;
            } else if (freeRamRatio > UNDERLOAD_FREE_RAM) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v3] classified ", n,
                " hosts using fixed free-ram bands");

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-ram-available-ratio";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-fixed";
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
