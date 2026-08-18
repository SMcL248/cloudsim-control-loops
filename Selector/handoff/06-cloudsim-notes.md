# 06 — CloudSim Notes (Observability, Version Drift, Library Behaviour)

## CloudSim Observability Constraint

CloudSim models the **provisioning layer**, not the observation layer. The simulation tracks what was requested and what was allocated, but there is no native telemetry pipeline equivalent to Prometheus or collectd. Available signals at the entity API level are:

- **Demand pressure** — `vm.getTotalUtilizationOfCpuMips(now)`, `vm.getCurrentRequestedTotalMips()` — what VMs are requesting.
- **Provisioned capacity** — `host.getTotalMips()`, `host.getRam()` — static allocation values.
- **VM count** — `host.getGuestList().size()` — structural, not dynamic.
- **Cloudlet completion state** — finish times, lengths — available only after the fact.

Unavailable: actual scheduler-allocated MIPS per VM after time-sharing contention, queue depth, per-VM throughput degradation under overload.

### monitor_v1 / monitor_v2 — util vs demand are distinct metrics

`vm.getTotalUtilizationOfCpuMips(now)` (cpu-util) and `vm.getCurrentRequestedTotalMips()` (cpu-demand) return different values in practice. Observed at t=2300 with cloudlets actively running:

```
Host 0  cpu-util=0.0    cpu-demand=0.5
Host 1  cpu-util=0.0    cpu-demand=0.5
Host 2  cpu-util=0.0    cpu-demand=0.375
Host 3  cpu-util=0.0    cpu-demand=0.5
Host 4  cpu-util=0.0    cpu-demand=0.25
Host 5  cpu-util=0.125  cpu-demand=0.375
```

- **cpu-demand** (`getCurrentRequestedTotalMips()`) — what VMs are requesting. Deterministic and stable: reflects VM workload regardless of when in the time-share cycle the observation fires.
- **cpu-util** (`getTotalUtilizationOfCpuMips(now)`) — what the scheduler has actually allocated at this instant. Fluctuates based on scheduling timing relative to the observation tick. Can read 0.0 even when heavy cloudlet work is in progress.

The earlier conclusion that these two calls are equivalent under `UtilizationModelFull` + `CloudletSchedulerTimeShared` was incorrect. They diverge significantly at runtime.

**Implication for the ground truth — RESOLVED 2026-07-01**: the ground truth previously used `getTotalUtilizationOfCpuMips(now)`. Confirmed against CloudSim source that this is worse than "noisy": `CloudletScheduler.getTotalUtilizationOfCpu(time)` sums `cl.getUtilizationOfCpu(time)` over the full exec list with no normalisation, so under `UtilizationModelFull` it is literally a count of concurrently-executing cloudlets, and `getMips()` (per-PE) turns that headcount into pseudo-MIPS units disconnected from host capacity — observed readings hit 2.75× host capacity at t=100. Switched to `getCurrentRequestedTotalMips()`, which sums `currentMipsShare` — the allocation actually granted by `VmSchedulerTimeShared`, which hard-rejects any allocation that would exceed the host's total MIPS ("this scheduler does not support over-subscription"). This makes the new signal bounded to ≤1.0 by construction, not just empirically well-behaved. Re-run complete — new baseline 0.222222, old baseline (2.687) invalidated and retained only for historical comparison. See "5×5×5×3 Run — Demand-Based Ground Truth (Current)" above.

**Implication for the thesis**: cpu-util and cpu-demand are genuinely distinct observability signals in CloudSim, providing different information about system state. The LLM correctly labelled each module's intended behaviour and the GUID distinction is semantically meaningful, not a redundant labelling artefact.

**Correction (2026-07-07)**: "deterministic and stable: reflects VM workload" above is superseded — `getCurrentRequestedTotalMips()` is stable because it reflects VM *placement* (the host scheduler's allocated MIPS share), not because it tracks live workload. It does not fall to zero when a VM's cloudlets finish; it only changes when VM population on a host changes. See "CPU Demand Metric Is a Placement Signal, Not an Activity Signal" under the Power-Aware Family section above for the full root-cause trace.

## VM PE-Count Scaling — Verified Throughput Lever via Contention (2026-07-09)

Confirmed by direct read of `CloudletScheduler.java` (abstract base) and `CloudletSchedulerTimeShared.java`, resolving an open question about whether scaling a VM's PE count (as opposed to its per-PE MIPS rating) would be a meaningful action.

**Mechanism** — `CloudletScheduler.updateCurrentCapacity()`:
```java
capacity /= Math.max(pesInUse, cpus);
```
`cpus = currentMipsShare.size()` — the VM's currently host-granted PE count. `pesInUse` = sum of `cl.getNumberOfPes()` across every cloudlet currently executing with remaining work. This is genuinely contention-aware — the "Simple policy, there is no real scheduling involved" comment on `CloudletSchedulerTimeShared.getTotalCurrentAvailableMipsForCloudlet` (`= getCurrentCapacity() * cl.getNumberOfPes()`) is about the lack of per-cloudlet *prioritisation*, not about whether PE contention matters — it clearly does, via this divisor in the base class.

**Threshold behaviour**: VM PE count only affects throughput when `pesInUse > cpus` — cloudlets on that VM collectively demanding more PEs than the VM has. Below that threshold, `capacity`'s numerator and denominator scale together and net out to a constant per-PE rate; adding PEs does nothing. Above it, increasing the VM's PE count proportionally increases every one of its cloudlets' allocated MIPS.

**Not hypothetical for this project's current scenario.** VMs are configured with `pesNumber=1` (see 07-limitations.md, `MIPS_TIERS`/`PeProvisionerSimple` mismatch finding), and with 60 cloudlets across 12 VMs under `CloudletSchedulerTimeShared`, multiple cloudlets executing concurrently on the same VM is the normal case. Any VM running ≥2 cloudlets simultaneously already has `pesInUse ≥ 2 > cpus = 1` — contention is very likely the *default state* right now, silently capping throughput on every busy VM, with nothing in the current module pool able to touch it.

**Load-bearing, not just an estimate**: `updateCloudletsProcessing` calls `getTotalCurrentAllocatedMipsForCloudlet` directly to advance `cl.updateCloudletFinishedSoFar` — the real simulated work-progress accounting, not a side calculation.

**Unresolved loose thread**: `CloudletSchedulerTimeShared.cloudletSubmit`'s returned ETA (`cl.getCloudletLength() / capacity`) doesn't multiply by `cl.getNumberOfPes()`, while `getEstimatedFinishTime` (used by the real progress accounting in `updateCloudletsProcessing`) correctly does via `getTotalCurrentAllocatedMipsForCloudlet`. Likely a submission-time-only cosmetic inconsistency rather than something affecting real simulated completion — not fully verified either way, low priority.

See 01-architecture.md, "Additional Action-Type Candidates" for how this feeds the VM PE-count-scaling action proposal.

---

## Host Storage — `getStorage()` Is a Depleting Pool, Not a Capacity Getter (2026-08-05, source-confirmed 2026-08-06)

While extending resource-utilization ground truth to storage (mirroring the existing RAM/BW/MIPS avg/peak/headroom/variance tracking, 04-family-power.md "Resource Utilization Ground Truth" and its since-added MIPS/variance/VM-level extensions), found storage doesn't share the other three resources' accessor shape. RAM/BW/MIPS each expose a *total-capacity* getter (`host.getRam()`, `host.getBw()`, `host.getTotalMips()`) separate from an *available* getter (`host.getGuestRamProvisioner().getAvailableRam()`, the BW/MIPS equivalents) — utilization is `used/total`, headroom is the available getter directly. Storage has exactly one call anywhere in this project's history: `host.getStorage()`, used in `canMigrateGuestToHost` (`host.getStorage() >= vm.getSize()`) as an admission check.

**Source-confirmed 2026-08-06.** `Host.java`/`HostEntity.java` were located under the imported-knowledge project docs (not the connected workspace folder — the earlier "never found" gap was a search-scope miss, not a real absence). `Host.java` itself only shows `storage` as a plain field with a trivial getter/setter; the real mutation logic lives in `HostEntity`'s default methods:

- `guestCreate(GuestEntity guest)`: hard-gates on `getStorage() < guest.getSize()` (fails allocation by storage before touching RAM/BW/MIPS), then on success `setStorage(getStorage() - guest.getSize())`.
- `guestDeallocate(GuestEntity guest)`, `guestDestroyAll()`, `removeMigratingInGuest()`: each `setStorage(getStorage() + guest.getSize())`, restoring the pool.
- `addMigratingInGuest()`: same depleting decrement as `guestCreate`, with a hard `System.exit(0)` if storage is insufficient (consistent with this file's other library-crash findings, 2026-07-28 entry).

This confirms the original inference exactly: `getStorage()` plays the same role `getAvailableMips()`/`getAvailableRam()`/`getAvailableBw()` play for the other three resources — a depleting/available pool, not a static total. No paired `getTotalStorage()`/`getStorageCapacity()` exists.

**Consequence for the storage utilization/headroom/variance metrics (`Selector.java`/`SelectorNoLogs.java`, 2026-08-05).** The shipped formulas are correct as-is, no change needed: `hostStorageUsed` is computed the same way `hostRamUsed`/`hostBwUsed`/`hostMipsUsed` already are (`Σ vm.getSize()` across `host.getGuestList()`); total is *derived* as `hostStorageUsed + host.getStorage()` rather than read directly. Headroom (`avg_host_free_storage`) uses `host.getStorage()` directly, same pattern as the other three headroom metrics.

---



---

## VM Placement Mechanics — Aggregate MIPS Pool, Not a PE-Slot Ceiling; Exhaustive-Search Allocation (2026-08-11)

Investigated while calibrating `SelectorLoadLever.java`'s `CONTENTION_LOAD` lever (full narrative in 05-scenarios.md, "Contention Lever Calibration"). Two mechanisms confirmed by direct source read, both correcting assumptions this project had been implicitly relying on.

**`VmSchedulerTimeShared` (the `VmScheduler` implementation used throughout this project's hosts) does not allocate PEs 1:1 to VMs — it pools a host's PEs into one aggregate MIPS budget.** `allocatePesForGuest(vmUid, mipsShareRequested)` runs exactly two checks: (1) each individual virtual PE's requested MIPS must not exceed a single physical PE's MIPS ceiling (`if (mips > peMips) return false;` — this is a hard, per-VM structural constraint, confirmed to be the mechanism behind the legacy-host-exclusion finding in 05-scenarios.md), and (2) the VM's total requested MIPS must not exceed the host's remaining aggregate MIPS (`if (getAvailableMips() < totalRequestedMips) return false;`). There is no rule anywhere capping the *number* of VMs a host can carry by its PE count — a host can host arbitrarily many small VMs, limited only by aggregate MIPS, PE-count only matters via check (1)'s per-VM ceiling. Any future capacity reasoning in this project should use aggregate host MIPS as the resource pool, not PE-slot count.

**VM-to-host placement (`VmAllocationWithSelectionPolicy` + `SelectionPolicyCustomRandom`, the `RANDOM_PLACEMENT=true` path) is a genuine exhaustive search over all hosts, not a single lazy random draw.** `SelectionPolicyCustomRandom.select()` itself does no suitability checking — it only excludes already-tried candidates and picks uniformly at random among what's left. The exhaustive part is in the caller, `VmAllocationWithSelectionPolicy.findHostForGuest`: a loop that calls `select()`, checks `isSuitableForGuest`, and on failure adds the host to the excluded set and retries, up to `getHostList().size()` tries before returning `null`. A VM only fails to place when *every* host in the datacenter has been tried and rejected — confirmed via source, not inferred from behaviour. This closes out a standing open question about whether VM-creation failures under high `CONTENTION_LOAD` were a genuine capacity signal or an artefact of the random selection policy giving up early; they are genuine.
