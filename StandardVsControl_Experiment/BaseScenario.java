package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

/**
 * Shared setup logic for both broker scenarios.
 * Subclasses implement createBroker() and getName() only.
 */
public abstract class BaseScenario {

    protected static final int NUM_VMS        = 5;
    protected static final int NUM_CLOUDLETS  = 10;
    protected static final long MIN_LENGTH    = 10_000L;
    protected static final long MAX_LENGTH    = 100_000L;

    /**
     * Run a full simulation with the given seed.
     * @return makespan — finish time of the last cloudlet to complete.
     */
    public double run(long seed) throws Exception {

        // Fresh CloudSim state for every run
        CloudSim.init(1, Calendar.getInstance(), false);

        createDatacenter("Datacenter_0");
        createDatacenter("Datacenter_1");

        DatacenterBroker broker = createBroker();
        int brokerId = broker.getId();

        List<Vm>       vms       = createVMs(brokerId);
        List<Cloudlet> cloudlets = createCloudlets(brokerId, seed);

        broker.submitGuestList(vms);
        broker.submitCloudletList(cloudlets);

        CloudSim.startSimulation();
        List<Cloudlet> finished = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        return finished.stream()
                .mapToDouble(Cloudlet::getExecFinishTime)
                .max()
                .orElse(0.0);
    }

    /** Subclasses provide their specific broker. */
    protected abstract DatacenterBroker createBroker() throws Exception;

    /** Human-readable label used in CSV output. */
    public abstract String getName();

    // ------------------------------------------------------------------ //

    private List<Vm> createVMs(int brokerId) {
        List<Vm> list = new ArrayList<>();
        for (int i = 0; i < NUM_VMS; i++) {
            list.add(new Vm(
                    i, brokerId,
                    250,   // mips
                    1,     // pesNumber
                    512,   // ram MB
                    1000,  // bw
                    10000, // image size MB
                    "Xen",
                    new CloudletSchedulerTimeShared()));
        }
        return list;
    }

    private List<Cloudlet> createCloudlets(int brokerId, long seed) {
        List<Cloudlet> list    = new ArrayList<>();
        Random         rng     = new Random(seed);
        UtilizationModel full  = new UtilizationModelFull();

        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            long length = MIN_LENGTH + (long)(rng.nextDouble() * (MAX_LENGTH - MIN_LENGTH));
            Cloudlet c  = new Cloudlet(i, length, 1, 300, 300, full, full, full);
            c.setUserId(brokerId);
            list.add(c);
        }
        return list;
    }

    private Datacenter createDatacenter(String name) throws Exception {
        List<Host> hosts = new ArrayList<>();
        int mips = 1000, ram = 16384, bw = 10000;
        long storage = 1_000_000L;

        // Host 0 — quad-core
        List<Pe> peList1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) peList1.add(new Pe(i, new PeProvisionerSimple(mips)));
        hosts.add(new Host(0,
                new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
                storage, peList1, new VmSchedulerTimeShared(peList1)));

        // Host 1 — dual-core
        List<Pe> peList2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) peList2.add(new Pe(i, new PeProvisionerSimple(mips)));
        hosts.add(new Host(1,
                new RamProvisionerSimple(ram), new BwProvisionerSimple(bw),
                storage, peList2, new VmSchedulerTimeShared(peList2)));

        DatacenterCharacteristics chars = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hosts, 10.0, 3.0, 0.05, 0.1, 0.1);

        return new Datacenter(name, chars,
                new VmAllocationPolicySimple(hosts), new LinkedList<>(), 0);
    }
}
