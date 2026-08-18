package org.cloudbus.cloudsim.selectionPolicies;

import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFull;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyWorstFit;

public class SelectionPolicyLeastFullByCapacity<T extends HostEntity> implements SelectionPolicy<T> {
    @Override
    public T select(List<T> candidates, Object obj, Set<T> excludedCandidates) {
        double maxAvailable = -1;
        T selectedHost = null;

        for (T hostCandidate : candidates) {
            if (excludedCandidates.contains(hostCandidate)) {
                continue;
            }

            double hostAvailable = hostCandidate.getGuestScheduler().getAvailableMips();

            if (hostAvailable > maxAvailable) {
                maxAvailable = hostAvailable;
                selectedHost = hostCandidate;
            }
        }
        return selectedHost;
    }
}