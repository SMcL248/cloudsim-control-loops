package org.cloudbus.cloudsim.selectionPolicies;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class SelectionPolicyCustomRandom<T extends HostEntity> implements SelectionPolicy<T> {

    private final Random rng;

    public SelectionPolicyCustomRandom(long seed){
        this.rng = new Random(seed);
    }

    @Override
    public T select(List<T> candidates, Object obj, Set<T> excludedCandidates){

        List<T> eligble = new ArrayList<T>();
        for (T host : candidates){

            if (!excludedCandidates.contains(host)){
                eligble.add(host);
            }

        }

        if (eligble.isEmpty()){
            return null;
        }

        return eligble.get(rng.nextInt(eligble.size()));

    }

    
}
