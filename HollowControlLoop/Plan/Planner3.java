package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.cloudbus.cloudsim.core.HostEntity;


public class Planner3 implements Planner<Diagnosis<HostEntity>, List<VmScalingAction>>{

    @Override
    public List<VmScalingAction> plan(Diagnosis<HostEntity> diagnosis, ReadSpace readSpace){

        List<VmScalingAction> scalingActions = new ArrayList<>();

        Set<HostEntity> underloadedHosts = new HashSet<>();
        Set<HostEntity> overloadedHosts = new HashSet<>();

        List<HostEntity> hosts = readSpace.getAllHosts();

        // Iterate by host in datacenter to find overloaded
        for (HostEntity host : hosts){

            HostEntity underloadedHost = null;
            LoadState state = diagnosis.classification().get(host);

            if (state == LoadState.OVERLOADED){

                overloadedHosts.add(host);

                // Iterate by host in datacenter again to find underloaded match
                for (HostEntity host2 : hosts){

                    LoadState state2 = diagnosis.classification().get(host2);

                    if (state2 == LoadState.UNDERLOADED && !underloadedHosts.contains(host2)){

                        underloadedHost = host2;

                        underloadedHosts.add(underloadedHost);

                        break;

                    }

                }

            }

        }

        if (underloadedHosts.size() < overloadedHosts.size()){

            scalingActions.add(new CreateVmAction(0));

        }

        return scalingActions;
    }

    @Override
    public String inputGuid() {
        return "host-loadstate";
    }

    @Override
    public String outputGuid() {
        return "host-scaleup";
    }
}
