package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Analyser v7 - VM-level utilisation classifier against per-VM capacity
 * headroom.
 *
 * Level        : VM (level 3)
 * Metric       : Per-VM CPU utilisation ratio, range approx [0, 1].
 * Threshold    : Dynamic and per-entity - rather than comparing every VM
 *                against one global cut-off, each VM's own requested-to-
 *                maximum MIPS ratio (its "headroom" from ReadSpace) is
 *                used as its personal overload boundary. A VM already
 *                using more than that headroom fraction is OVERLOADED;
 *                using less than half of it is UNDERLOADED. This lets
 *                small and large VM tiers carry different effective
 *                thresholds, supporting the throughput goal.
 * Startup rule : A VM still being instantiated is always BALANCED,
 *                since its metric reading is not yet meaningful.
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final double UNDERLOAD_FRACTION = 0.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        for (int i = 0; i < n; i++) {
            if (i >= vms.size()) {
                states[i] = LoadState.BALANCED;
                continue;
            }

            GuestEntity vm = vms.get(i);

            if (readSpace.isVmBeingInstantiated(vm)) {
                states[i] = LoadState.BALANCED;
                continue;
            }

            double maxMips = readSpace.getVmMaxMips(vm);
            double requestedMips = readSpace.getVmRequestedMips(vm);
            double headroomRatio = (maxMips > 0.0) ? requestedMips / maxMips : 1.0;
            headroomRatio = Math.min(Math.max(headroomRatio, 0.0), 1.0);

            double util = metrics[i];

            if (util > headroomRatio) {
                states[i] = LoadState.OVERLOADED;
            } else if (util < UNDERLOAD_FRACTION * headroomRatio) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v7] classified ", n,
                " vms using per-vm headroom thresholds");

        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-util-ratio";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-headroom";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}
