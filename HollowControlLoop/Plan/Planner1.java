package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Planner1 implements Planner<Diagnosis, List<MigrationPair>> {

    @Override
    public List<MigrationPair> plan(Diagnosis diagnosis) {
        List<MigrationPair> migrations = new ArrayList<>();

        GuestEntity mostLoaded = diagnosis.mostLoaded();
        GuestEntity leastLoaded = diagnosis.leastLoaded();

        if (mostLoaded != null && leastLoaded != null && mostLoaded != leastLoaded) {
            double largest = diagnosis.values().get(mostLoaded);
            double smallest = diagnosis.values().get(leastLoaded);

            if (largest > 2.0 * smallest && migrationWorthwhile(mostLoaded, leastLoaded, diagnosis.mean())) {
                migrations.add(new MigrationPair(mostLoaded, leastLoaded));
            }
        }

        return migrations;
    }

    private boolean migrationWorthwhile(GuestEntity fromVm, GuestEntity toVm, double meanWork) {

        double mostWork = 0;
        for (Cloudlet cloudlet : fromVm.getCloudletScheduler().getCloudletExecList()) {
            if (cloudlet.getRemainingCloudletLength() > mostWork) {
                mostWork = cloudlet.getRemainingCloudletLength();
            }
        }

        if (mostWork == 0) {
            return false;
        }

        int fromCount = fromVm.getCloudletScheduler().getCloudletExecList().size();
        int toCount = toVm.getCloudletScheduler().getCloudletExecList().size();

        double effectiveMipsFrom = fromVm.getMips() / Math.max(fromCount, 1);
        double effectiveMipsTo = toVm.getMips() / Math.max(toCount + 1, 1);

        double improvement = (mostWork / effectiveMipsFrom) - (mostWork / effectiveMipsTo);

        double MINIMUM_IMPROVEMENT = meanWork * 0.05;

        return improvement > MINIMUM_IMPROVEMENT;
    }
}