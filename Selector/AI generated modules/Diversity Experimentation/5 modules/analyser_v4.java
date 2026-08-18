package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Variant 4 - Host level, operational-state override plus fixed thresholds.
 * A host that is down, failed, or permanently dead is doing no useful work no
 * matter what its power reading suggests, so its operational state overrides
 * the numeric classification. Any host still in service is then classified
 * against fixed power-ratio thresholds.
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-power-consumption-ratio-of-max-power";
    private static final String OUTPUT_SEMANTIC = "host-load-classification-balanced-under-over";

    private static final double OVERLOAD_THRESHOLD = 0.75;
    private static final double UNDERLOAD_THRESHOLD = 0.30;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        List<HostEntity> hosts = readSpace.getAllHosts();

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;
        int stateOverridden = 0;

        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            LoadState state;

            if (readSpace.isHostPermanentlyDead(host)
                    || readSpace.isHostFailed(host)
                    || readSpace.isHostPoweredDown(host)) {
                state = LoadState.UNDERLOADED;
                stateOverridden++;
            } else {
                double powerRatio = metrics[i];
                if (powerRatio > OVERLOAD_THRESHOLD) {
                    state = LoadState.OVERLOADED;
                } else if (powerRatio < UNDERLOAD_THRESHOLD) {
                    state = LoadState.UNDERLOADED;
                } else {
                    state = LoadState.BALANCED;
                }
            }

            states[i] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            else if (state == LoadState.UNDERLOADED) underloaded++;
            else balanced++;
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] power-ratio host classification complete -> ",
                n, " hosts, overloaded=", overloaded, ", underloaded=", underloaded, ", balanced=", balanced,
                ", stateOverridden=", stateOverridden);

        return states;
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
