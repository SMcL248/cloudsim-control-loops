package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Analyser8 implements Analyser<double[], LoadState[]>{

    private static final double UPPER_THRESHOLD = 0.8;
    private static final double LOWER_THRESHOLD = 0.2;
    private int actionableCycles = 0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {

        double now = readSpace.getNow();

        LoadState[] classification = new LoadState[metrics.length];

        List<HostEntity> hosts = readSpace.getAllHosts();

        int i = 0;
        //Iterate by Host
        for (HostEntity host : hosts) {

            // Detemine load level
            if (metrics[i] > UPPER_THRESHOLD) {
                classification[i] = LoadState.OVERLOADED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is overloaded.");
            } else if (metrics[i] < LOWER_THRESHOLD) {
                classification[i] = LoadState.UNDERLOADED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is underloaded.");
            } else {
                classification[i] = LoadState.BALANCED;
                Log.printlnConcat(now, ": Host #", host.getId(), " is balanced.");
            }

            i++;
        }

        for (LoadState l : classification){
            if (l == LoadState.OVERLOADED){
                actionableCycles++;
                break;
            }
        }


        return classification;

    }

    @Override
    public String inputGuid() {
        return "host-cpu";
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }

}