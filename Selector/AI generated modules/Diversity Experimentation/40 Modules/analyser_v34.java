package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v34 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        if (n < 3) {
            for (int i = 0; i < n; i++) result[i] = LoadState.BALANCED;
        } else {
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) order[i] = i;
            Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));
            double totalRange = metrics[order[n - 1]] - metrics[order[0]];
            double avgGap = (n > 1) ? totalRange / (n - 1) : 0.0;
            int overCount = 0, underCount = 0;
            for (int rankPos = 0; rankPos < n; rankPos++) {
                int idx = order[rankPos];
                double leftGap = (rankPos > 0) ? metrics[idx] - metrics[order[rankPos - 1]] : 0.0;
                double rightGap = (rankPos < n - 1) ? metrics[order[rankPos + 1]] - metrics[idx] : 0.0;
                double localGap = Math.max(leftGap, rightGap);
                boolean isUpperTail = rankPos > n / 2;
                if (avgGap > 1e-9 && localGap > 2.5 * avgGap) {
                    result[idx] = isUpperTail ? LoadState.OVERLOADED : LoadState.UNDERLOADED;
                    if (isUpperTail) overCount++; else underCount++;
                } else {
                    result[idx] = LoadState.BALANCED;
                }
            }
            Log.printlnConcat(now, ": [analyser_v34] classified ", n, " vms via nearest-neighbor spacing density outliers (avgGap=", avgGap, ", over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-nearest-neighbor-gap";
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
