package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Variant 5 - VM level - Stateful hysteresis classification.
 * Assumes the input metric is per-VM rolling-mean CPU utilisation
 * (a 30-reading average), in [0,1]. Unlike the stateless variants,
 * this analyser remembers each VM's previous LoadState across calls
 * and applies asymmetric enter/exit bands, so a VM must cross a
 * further threshold to leave OVERLOADED/UNDERLOADED than it did to
 * enter it. This avoids rapid flapping between states when a VM's
 * utilisation hovers near a single boundary.
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;

    private static final double ENTER_OVERLOAD = 0.80;
    private static final double EXIT_OVERLOAD = 0.65;
    private static final double ENTER_UNDERLOAD = 0.20;
    private static final double EXIT_UNDERLOAD = 0.35;
    private static final double BOOTSTRAP_HIGH = 0.70;
    private static final double BOOTSTRAP_LOW = 0.30;

    private final Map<Integer, LoadState> previousState = new HashMap<>();

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(n, vms.size());

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            Integer vmId = i < limit ? readSpace.getId(vms.get(i)) : null;
            LoadState prior = vmId != null ? previousState.get(vmId) : null;

            LoadState next;
            if (prior == LoadState.OVERLOADED) {
                next = util >= EXIT_OVERLOAD ? LoadState.OVERLOADED : LoadState.BALANCED;
            } else if (prior == LoadState.UNDERLOADED) {
                next = util <= EXIT_UNDERLOAD ? LoadState.UNDERLOADED : LoadState.BALANCED;
            } else if (prior == LoadState.BALANCED) {
                if (util >= ENTER_OVERLOAD) {
                    next = LoadState.OVERLOADED;
                } else if (util <= ENTER_UNDERLOAD) {
                    next = LoadState.UNDERLOADED;
                } else {
                    next = LoadState.BALANCED;
                }
            } else {
                // no history yet - bootstrap with the midpoint bands
                if (util >= BOOTSTRAP_HIGH) {
                    next = LoadState.OVERLOADED;
                } else if (util <= BOOTSTRAP_LOW) {
                    next = LoadState.UNDERLOADED;
                } else {
                    next = LoadState.BALANCED;
                }
            }

            result[i] = next;
            if (next == LoadState.OVERLOADED) {
                overloaded++;
            } else if (next == LoadState.UNDERLOADED) {
                underloaded++;
            }
            if (vmId != null) {
                previousState.put(vmId, next);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] classified ", n,
            " VMs via hysteresis banding -> overloaded=", overloaded, ", underloaded=",
            underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuUtilizationRollingMean-30reading";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState-hysteresisBand";
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
