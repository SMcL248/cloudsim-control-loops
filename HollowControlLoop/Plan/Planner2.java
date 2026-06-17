package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;


public class Planner2 implements Planner<Diagnosis<HostEntity>, List<VmMigrationPair>>{

    @Override
    public List<VmMigrationPair> plan(Diagnosis<HostEntity> diagnosis){

        List<VmMigrationPair> migrations = new ArrayList<>();
        Set<HostEntity> allocatedTargets = new HashSet<>();

        // Group hosts by datacenter
        Map<Integer, List<HostEntity>> hostsByDatacenter = new HashMap<>();
        for (var entry : diagnosis.datacenterIds().entrySet()) {
            hostsByDatacenter.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                            .add(entry.getKey());
        }

        // Iterate by datacenter
        for (Integer id : hostsByDatacenter.keySet()){

            // Iterate by host in datacenter to find overloaded
            for (HostEntity host : hostsByDatacenter.get(id)){

                HostEntity targetHost = null;
                LoadState state = diagnosis.classification().get(host);

                if (state == LoadState.OVERLOADED){

                    // Iterate by host in datacenter again to find underloaded match
                    for (HostEntity host2 : hostsByDatacenter.get(id)){

                        LoadState state2 = diagnosis.classification().get(host2);

                        if (state2 == LoadState.UNDERLOADED && !allocatedTargets.contains(host2)){

                            targetHost = host2;
                            double max_mips = 0;
                            GuestEntity targetVm = null;

                            //Find VM using most MIPS
                            for (GuestEntity vm : host.getGuestList()){

                                if (vm.getCurrentRequestedTotalMips() > max_mips){
                                    targetVm = vm;
                                    max_mips = vm.getCurrentRequestedTotalMips();
                                }
                            }

                            if (targetVm == null){
                                break;
                            }

                            if (targetHost.isSuitableForGuest(targetVm)){
                                allocatedTargets.add(targetHost);
                                migrations.add(new VmMigrationPair(targetVm, targetHost));
                                break;
                            }

                        }

                    }

                }

            }
    
        }

        return migrations;
    }
}
