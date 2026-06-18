package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Planner1 implements Planner<Diagnosis<GuestEntity>, List<MigrationPair>> {

    @Override
    public List<MigrationPair> plan(Diagnosis<GuestEntity> diagnosis) {

        List<MigrationPair> migrations = new ArrayList<>();

        // Derive mostLoaded and leastLoaded from values and classification
        GuestEntity mostLoaded = null;
        GuestEntity leastLoaded = null;
        double largest = 0;
        double smallest = Double.MAX_VALUE;

        for (var entry : diagnosis.values().entrySet()) {
            GuestEntity vm = entry.getKey();
            double value = entry.getValue();
            LoadState state = diagnosis.classification().get(vm);

            if (state == LoadState.OVERLOADED && value > largest) {
                largest = value;
                mostLoaded = vm;
            }
            if (value < smallest) {
                smallest = value;
                leastLoaded = vm;
            }
        }

        double mean = diagnosis.values().values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        if (mostLoaded != null && leastLoaded != null && mostLoaded != leastLoaded) {

            double mostWork = 0;
            Cloudlet cloudletToMigrate = null;
            
            // Find largest cloudlet on overloaded VM
            for (Cloudlet cloudlet : mostLoaded.getCloudletScheduler().getCloudletExecList()) {
                if (cloudlet.getRemainingCloudletLength() > mostWork) {
                    mostWork = cloudlet.getRemainingCloudletLength();
                    cloudletToMigrate = cloudlet;
                }
            }

            // Is migration worthwhile?
            if (largest > 2.0 * smallest && migrationWorthwhile(cloudletToMigrate, mostLoaded, leastLoaded, mean)) {

                migrations.add(new MigrationPair(cloudletToMigrate, mostLoaded, leastLoaded));

            }
        }

        return migrations;
    }

    private boolean migrationWorthwhile(Cloudlet cloudletToMigrate, GuestEntity fromVm, GuestEntity toVm, double meanWork) {

        double work = cloudletToMigrate.getRemainingCloudletLength();

        if (work == 0) {
            return false;
        }

        int fromCount = fromVm.getCloudletScheduler().getCloudletExecList().size();
        int toCount = toVm.getCloudletScheduler().getCloudletExecList().size();

        double effectiveMipsFrom = fromVm.getMips() / Math.max(fromCount, 1);
        double effectiveMipsTo = toVm.getMips() / Math.max(toCount + 1, 1);

        double improvement = (work / effectiveMipsFrom) - (work / effectiveMipsTo);

        double MINIMUM_IMPROVEMENT = meanWork * 0.05;

        return improvement > MINIMUM_IMPROVEMENT;
        
    }

}