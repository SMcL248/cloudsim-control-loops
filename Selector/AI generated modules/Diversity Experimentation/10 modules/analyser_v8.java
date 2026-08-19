package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilFraction-instantaneous";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-hysteresisMigrationAware";

    private static final double ENTER_OVERLOAD = 0.80;
    private static final double EXIT_OVERLOAD = 0.60;
    private static final double ENTER_UNDERLOAD = 0.20;
    private static final double EXIT_UNDERLOAD = 0.40;

    // Remembers each VM's last classification (keyed by VM id) so thresholds
    // can be asymmetric (hysteresis) and avoid rapidly flapping between
    // states from one control loop tick to the next.
    private final Map<Integer, LoadState> previousState = new HashMap<Integer, LoadState>();

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int overloadedCount = 0;
        int underloadedCount = 0;
        int migratingSkipped = 0;

        for (int i = 0; i < n; i++) {
            GuestEntity vm = (i < vms.size()) ? vms.get(i) : null;

            if (vm != null && readSpace.isVmMigrating(vm)) {
                // Utilisation readings are unreliable mid-migration; hold the
                // VM at BALANCED and leave its hysteresis memory untouched.
                result[i] = LoadState.BALANCED;
                migratingSkipped++;
                continue;
            }

            int vmId = (vm != null) ? readSpace.getId(vm) : i;
            LoadState last = previousState.get(vmId);
            double util = metrics[i];

            LoadState next;
            if (last == LoadState.OVERLOADED) {
                next = (util >= EXIT_OVERLOAD) ? LoadState.OVERLOADED : LoadState.BALANCED;
            } else if (last == LoadState.UNDERLOADED) {
                next = (util <= EXIT_UNDERLOAD) ? LoadState.UNDERLOADED : LoadState.BALANCED;
            } else if (util >= ENTER_OVERLOAD) {
                next = LoadState.OVERLOADED;
            } else if (util <= ENTER_UNDERLOAD) {
                next = LoadState.UNDERLOADED;
            } else {
                next = LoadState.BALANCED;
            }

            result[i] = next;
            previousState.put(vmId, next);

            if (next == LoadState.OVERLOADED) {
                overloadedCount++;
            } else if (next == LoadState.UNDERLOADED) {
                underloadedCount++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] classified ", n,
                " VMs with hysteresis thresholds (", overloadedCount, " overloaded, ",
                underloadedCount, " underloaded, ", migratingSkipped,
                " skipped mid-migration).");

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
