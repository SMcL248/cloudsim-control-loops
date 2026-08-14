package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ReadSpace {

    Integer getDatacenterFor(int vmId);
    List<GuestEntity> getVmList();
    int getUserId();
    List<HostEntity> getAllHosts();
    double getNow();
    int[] getMipsTiers();
    int[] getRamTiers();
    int[] getBwTiers();
    double getHostCapacity(GuestEntity vm);     
    boolean hostHasFreePe(HostEntity host);
    boolean isHostFailed(HostEntity host);
    Cloudlet getCloudletById(int cloudletId);
    GuestEntity getVmById(int vmId);
    HostEntity getHostById(int hostId);
    double getNextMipsTier(GuestEntity vm);
    boolean isHostPermanentlyDead(HostEntity host);
    double getHostAvailableRam(HostEntity host);
    double getHostAvailableBw(HostEntity host);
    double getHostTotalRam(HostEntity host);
    double getHostTotalBw(HostEntity host);
    double getHostTotalMips(HostEntity host);
    List<Cloudlet> getVmCloudletList(GuestEntity vm);
    long getRemainingLength(Cloudlet cl);
    long getTotalLength(Cloudlet cl);
    List<Cloudlet> getCompletedCloudletList(); 
    double getVmEffectiveThroughput(GuestEntity vm);
    double getHostAvailableMips(HostEntity host);
    List<GuestEntity> getVmListForHost(HostEntity host);
    double getVmRequestedMips(GuestEntity vm);
    double getVmCpuUtil(GuestEntity vm);
    int getId(HostEntity host);
    int getId(GuestEntity vm);
    int getId(Cloudlet cl);
    boolean isHostSuitableForGuest(HostEntity host, GuestEntity vm);
    boolean canMigrateGuestToHost(HostEntity host, GuestEntity vm);
    double getVmMips(GuestEntity vm);
    double getVmMaxMips(GuestEntity vm);
    List<Double> getVmMipsPerPe(GuestEntity vm);
    double getVmRam(GuestEntity vm);
    double getVmBw(GuestEntity vm);
    int getVmNumberOfPes(GuestEntity vm);
    int getCloudletNumberOfPes(Cloudlet cl);
    boolean isVmMigrating(GuestEntity vm);
    double getVmUtilizationMean(GuestEntity vm);
    double getVmUtilizationMad(GuestEntity vm);
    double getHostPower(HostEntity host);   
    double getHostPowerAtUtil(HostEntity host, double util);
    double getHostMaxPower(HostEntity host);
    double getHostEnergyEstimate(HostEntity host, double fromUtil, double toUtil, double time);
    double getTotalEnergyConsumedSoFar();
    double getTotalWorkProcessedSoFar();
    boolean isHostPoweredDown(HostEntity host);
    boolean isHostPoweringUp(HostEntity host);
    boolean isVmBeingInstantiated(GuestEntity vm);
    double getCloudletEstimatedFinishTime(GuestEntity vm, Cloudlet cl);
    List<Cloudlet> getActiveCloudlets();
    double getNextBwTier(GuestEntity vm);
    double getNextRamTier(GuestEntity vm);
    double getHostMipsPerPe(HostEntity host);
    int getHostPeCount(HostEntity host);
    int getNumberCloudletsAbandoned();

}