# Project Handoff

## Goal

Build a modular MAPE-K (Monitor, Analyser, Planner, Executor) control loop framework in CloudSim to test whether generative AI can produce functional autonomy engine modules. The approach:

1. Hand-code reference implementations of each module type
2. Use LLM prompts to generate variant modules
3. Build a permutation constructor that tests all valid module combinations
4. Identify which permutations produce functionally sound controllers

---

## Architecture

### Core Framework

- **`HollowedControl`** — generic broker extending `DatacenterBroker`, wires the MAPE pipeline together. Takes Monitor, Analyser, Planner, Executor as constructor arguments. Fires an observation cycle on a fixed `observationRate`. Implements `ActionSpace`.
- **`ReadSpace`** — read-only interface exposing simulation state. Methods: `getAllHosts()`, `getVmList()`, `getNow()`. Monitor receives this only — structurally prevented from writing.
- **`ActionSpace`** — extends `ReadSpace`. Adds write methods: `requestVmMigration(vm, host)`, `requestVmCreation(vm, datacenterId)`, `requestVmDestruction(vm)`. Executor receives this. `HollowedControl` implements `ActionSpace`, satisfying both interfaces.
- **`LoadState`** — enum: `OVERLOADED`, `UNDERLOADED`, `BALANCED`.

### Module Interfaces (simplified for generation)

```
Monitor      observe(ReadSpace)                    -> double[]
Analyser     analyse(double[], ReadSpace)          -> LoadState[]
Planner      plan(LoadState[], ReadSpace)          -> int[]
Executor     execute(int[], ActionSpace)           -> boolean
```

### Simplified Data Flow

- **`double[]`** — flat array of metric values. Index i corresponds to the i-th host in `readSpace.getAllHosts()`. Positional ordering is stable within a simulation run.
- **`LoadState[]`** — flat array of classification verdicts. Same positional ordering as the metric array.
- **`int[]`** — a single migration pair `[vmId, targetHostId]`. One migration per observation cycle. Sentinel value `{-1, -1}` signals no action.

### Interface Definitions

```java
public interface ReadSpace {
    List<HostEntity> getAllHosts();
    List<GuestEntity> getVmList();
    double getNow();
}

public interface ActionSpace extends ReadSpace {
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    void requestVmCreation(GuestEntity newVm, int datacenterId);
    Integer getDatacenterFor(GuestEntity vm);
    Integer getUserId();
}
```

`HollowedControl implements ActionSpace` — satisfies both interfaces. Monitor receives `ReadSpace` only, structurally prevented from writing. Executor receives full `ActionSpace`.

### GUID Convention

Each module declares input and output GUIDs describing what its data represents. The permutation constructor chains them:

```
monitor.outputGuid()   == analyser.inputGuid()
analyser.outputGuid()  == planner.inputGuid()
planner.outputGuid()   == executor.inputGuid()
```

#### GUID Naming Ruleset

Format: `<entity-level>-<metric>-<value-type>`

| Segment | Options | Notes |
|---|---|---|
| `entity-level` | `host`, `vm`, `cloudlet` | level of the entity being measured |
| `metric` | `cpu`, `vm`, `etc`, `mips` | the thing being measured |
| `value-type` | `util`, `demand`, `count`, `raw`, `loadstate`, `migration-pair` | the nature of the value |

Rules:
- All three segments are mandatory for monitor/analyser boundaries. Pipeline-internal GUIDs (`host-loadstate`, `host-migration-pair`) use two segments by convention.
- Segments are lowercase, hyphen-separated, no spaces.
- `util` — natural ratio of used/total, implies [0,1].
- `demand` — requested/available, can exceed 1.0.
- `count` — raw integer quantity, unbounded.
- `raw` — absolute value with no implied scale.

#### Current VM Migration GUID Family

```
Monitor      outputGuid: "host-cpu-util" | "host-cpu-demand" | "host-vm-count"
Analyser     inputGuid:  (one of the above)    outputGuid: "host-loadstate"
Planner      inputGuid:  "host-loadstate"      outputGuid: "host-migration-pair"
Executor     inputGuid:  "host-migration-pair"
```

**GUID encodes both composability and value contract.** An analyser declaring `inputGuid: "host-cpu-util"` can safely use [0,1] thresholds. One declaring `inputGuid: "host-vm-count"` knows it receives raw counts and must calibrate accordingly. The GUID is not just a matching key — it is the data contract.

**Normalisation policy:** Metrics that are naturally ratios (utilisation) are [0,1] without effort. Metrics that are absolute values (ETC, counts) stay raw — forcing normalisation onto the monitor embeds analytical decisions that belong in the analyser. Monitors must clamp naturally-ratio metrics to [0,1] via `Math.min(value, 1.0)` where oversubscription is possible (e.g. demand pressure).

### Key Design Decisions

- **`ReadSpace` / `ActionSpace` split** — enforced by the type system. Monitor receives `ReadSpace`, structurally prevented from writing. Analyser and Planner also receive `ReadSpace` for CloudSim API access. Executor receives full `ActionSpace`.
- **Single datacenter** — deliberate constraint for the generation experiment. Simplifies positional indexing and migration (always within-datacenter). Multi-datacenter support exists in the framework but is out of scope.
- **Flat primitive arrays** — replaces `Map<Entity, Map<String, Double>>` / `Diagnosis<E>`. Removes CloudSim entity objects from inter-module boundaries, reduces LLM prompt complexity, composability is purely a matter of GUID matching.
- **One migration per cycle** — Planner returns a single `int[]` pair. Firing multiple migrations before the next observation resolves would act on stale state. Can be relaxed later.
- **No WorldState record** — flattened into `ReadSpace`. One fewer concept to explain in prompts.
- **Boundary leak acknowledged** — `ReadSpace` exposes CloudSim entity objects. Modules can call arbitrary CloudSim APIs on them. Genuine confinement would require wrapper types — not worth the generation space cost. Flagged in thesis.
- **Framework is MAPE, not MAPE-K** — stateless per cycle. Flagged as future work.

---

## Completed Modules

Reference implementations are now written against the simplified array interface. The pre-simplification versions (rich-typed maps, `Diagnosis<E>`) are superseded.

### Control Loop 1 — Cloudlet Migration (CPU/ETC)
- **Monitor1** — measures ETC per VM.
- **Analyser1** — classifies VMs using mean ± stddev on ETC.
- **Planner1** — identifies most/least loaded VMs, evaluates migration worthwhileness via MIPS-based estimate, selects largest cloudlet from overloaded VM.
- **Executor1** — calls `moveCloudlet(...)` which sends native `CLOUDLET_MOVE` event.

### Control Loop 2 — VM Migration (host-level, single metric)
- **Monitor6** — iterates `readSpace.getAllHosts()`, computes `cpu_util = used_mips / total_mips` per host via CloudSim API. Returns `double[]`. `outputGuid: "host-cpu-util"`.
- **Analyser4** — fixed thresholds UPPER=0.8, LOWER=0.2. Classifies each index as OVERLOADED/UNDERLOADED/BALANCED. Returns `LoadState[]`. `inputGuid: "host-cpu-util"`, `outputGuid: "host-loadstate"`.
- **Planner4** — finds first overloaded host, finds first unused underloaded host, selects VM with highest MIPS demand from overloaded host, checks `host.isSuitableForGuest(vm)`. Returns `int[] {vmId, targetHostId}` or `{-1, -1}`. `inputGuid: "host-loadstate"`, `outputGuid: "host-migration-pair"`.
- **Executor4** — looks up VM and host by ID, calls `actionSpace.requestVmMigration(vm, targetHost)`. Returns false if sentinel received. `inputGuid: "host-migration-pair"`.

### Control Loop 3 — VM Scaling (CPU Utilisation)
- **Monitor2** — reused.
- **Analyser2** — reused, UPPER=0.6.
- **Planner3** — if overloaded hosts outnumber underloaded hosts, emits `CreateVmAction`. Scale-in not yet implemented.
- **Executor3** — checks `isVmCreationPending()`, constructs new `Vm` with hardcoded specs, calls `requestVmCreation(datacenterId)`.

#### Key CloudSim VM scaling notes
- `DatacenterBroker` does not natively support mid-simulation VM creation. `HollowedControl` extends this by adding new VMs to `getGuestList()` (not `getGuestsCreatedList()`) before sending `VM_CREATE`, so `processVmCreateAck` correctly populates `vmsToDatacentersMap`.
- A `vmCreationPending` flag (cleared in `processVmCreateAck`) prevents duplicate creation requests within the same cycle.

### LLM-Generated Modules — VM Migration Family

Generated independently using a two-file prompting approach: a persistent `system_context.md` (role, architecture, interfaces, constraints) and a per-module spec file (type, data contract, GUID strings, approved CloudSim API, variant count).

#### Current variants (3×3×3×3 = 81 permutations validated)

**Monitors**
- **monitor_v1** — CPU utilisation. `used_mips / total_mips`. `outputGuid: "host-cpu-util"`.
- **monitor_v2** — CPU demand pressure. `sum(vm.getCurrentRequestedTotalMips()) / host.getTotalMips()`. Clamped to 1.0. `outputGuid: "host-cpu-demand"`.
- **monitor_v3** — VM count. Raw count of resident VMs per host. `outputGuid: "host-vm-count"`.

**Analysers**
- **analyser_v1** — Fixed thresholds. `outputGuid: "host-loadstate"`.
- **analyser_v2** — Statistical (mean ± stddev) with absolute floor. `outputGuid: "host-loadstate"`.
- **analyser_v3** — Statistical (mean ± stddev), wider sensitivity. `outputGuid: "host-loadstate"`.

**Planners**
- **planner_v1** — Overload relief. Triggers on OVERLOADED. Selects highest-MIPS VM, prefers UNDERLOADED destination, falls back to BALANCED.
- **planner_v2** — Consolidation. Triggers on UNDERLOADED. Selects lowest-MIPS VM, prefers BALANCED destination, falls back to another UNDERLOADED.
- **planner_v3** — Overload relief variant.

**Executors**
- **executor_v1, v2, v3** — All functionally equivalent in current scenario. Resolve VM and host by ID, call `requestVmMigration()`. Executor slot confirmed as non-discriminating with single migration pair contract.

#### Observations from generation runs

- **Structural compliance: 100%** — all modules compiled and integrated without modification.
- **Hallucinated CloudSim API** — initial Monitor generation called `host.getAvailableMips()` which does not exist. Resolved by removing from approved API. Correct pattern: iterate `host.getGuestList()` and sum `vm.getTotalUtilizationOfCpuMips(now)`. Confirms approved API list is a necessary prompting component.
- **Character encoding** — LLM used em dash in log strings, rendered as `ÔÇö`. Fixed by adding ASCII-only constraint to system context.
- **Semantic gap producing livelock** — `isSuitableForGuest()` checks provisioned capacity, not actual utilisation. Monitor measures actual utilisation. Mismatch produces persistent unresolvable overload. Structurally correct, behaviourally pathological. Not detectable by GUID matching. Documented as thesis finding.
- **RAM and BW dead signals** — CloudSim allocates RAM and BW statically. Monitors measuring these metrics return constant values every cycle. Analysers see zero variance; nothing is ever classified OVERLOADED. Environmental constraint, not a module defect. Reflected in GUID design — RAM/BW GUIDs excluded from the VM migration family.
- **monitor_v2 (cpu-demand) — dead signal in this scenario** — demand pressure values sit in the BALANCED range throughout. Likely a consequence of the specific workload configuration rather than a fundamental CloudSim limitation.
- **monitor_v3 (vm-count) + analyser_v1/v2 — signal detected, no action** — Raw VM counts interpreted by CPU-calibrated analysers over-classify all hosts as OVERLOADED. Planner finds no valid destination. Feasibility check silently absorbs the mismatch. 0% conversion rate across 11 actionable cycles.
- **planner_v2 trigger mismatch** — Consolidation planner triggers on UNDERLOADED, not OVERLOADED. This caused conversion rates >100% under the original `actionableCycles` definition (OVERLOADED-only). Fixed by redefining actionable cycles as OVERLOADED or UNDERLOADED detected.
- **Executor slot non-discriminating** — All three executor variants produce identical results across all 81 permutations. The `int[]{vmId, hostId}` contract leaves almost no variation space — any correct executor calls `requestVmMigration()` identically. Executor generation deprioritised; variation effort concentrated on Monitor, Analyser, Planner.

---

## Dissertation Context — Muiz Rusli (From Intent to Implementation)

### Key Difference from This Project

Muiz's system is a **single-function replacement policy**: given cache state, return the item to evict. One module slot, one API type, all modules trivially interchangeable. No pipeline, no closed feedback loop.

This project tests **LLM generation of control loop components that must interoperate across a sequential four-stage pipeline and produce emergent closed-loop behaviour**. The difficulty scales with inter-stage coupling. Muiz's system is also not MAPE — it lacks the observe→analyse→plan→act structure.

### What the Dissertation Validates

- **Thin interfaces work.** 100% structural compliance came from module spec + API catalogue as prompting context, not runtime enforcement.
- **The API catalogue is a prompting artefact, not a runtime mechanism.**
- **Structural diversity degrades at large N.** LLM output converges toward a template at ~N=40 even with thin constraints.
- **GUID for composability.** Modules declare which GUID they implement; the permutation constructor uses GUIDs to enumerate valid combinations.

### What the Dissertation Does Not Cover

- **Cross-GUID composition** — Muiz had only one API type so cross-family pairing was not a concept.
- **Multi-stage semantic coupling** — semantic mismatches producing silent failures rather than compile errors.

### Cross-GUID Composition as Experimental Angle

Deliberately composing modules from different GUID families produces controllers that compile but are semantically mismatched. Three-category experimental design:

1. **Valid-GUID permutations** — same semantic family; expected to be sound
2. **Cross-GUID permutations** — intentional mismatches; characterising *how* they fail is a contribution
3. **LLM-generated modules** — which category do they naturally fall into, and at what rate?

---

## Permutation Constructor

`Constructor.java` — iterates all module combinations, runs each through the standard simulation scenario, collects `SimulationResult` records, and prints a summary after all runs complete.

### Design
- **Module registries** — `monitorDict`, `analyserDict`, `plannerDict`, `executorDict` as static arrays. Add new variants here.
- **GUID validation** — checked per combination but does not gate execution. Incompatible combinations still run — characterising how they fail is part of the experiment.
- **Fresh instances per run** — modules instantiated via reflection (`getDeclaredConstructor().newInstance()`) inside the permutation loop to prevent counter accumulation across runs.
- **`SimulationResult` record** — `monitorId`, `analyserId`, `plannerId`, `executorId` (class simple names), `makespan`, `actionableCycles`, `actionsExecuted`, `conversionRate`.
- **Statistics output** — four metrics printed after all runs: % compatible, % failed, % compatible-and-failed, % incompatible-but-succeeded.
- **Deferred logging** — `logResult` called after all simulations complete.

### SimulationResult metrics
- `makespan` — max cloudlet finish time. `-1` signals failure. **Confirmed non-discriminating** — identical across all 81 permutations regardless of controller behaviour.
- `actionableCycles` — cycles where Analyser detected at least one OVERLOADED or UNDERLOADED state. Definition updated from OVERLOADED-only to capture consolidation planners correctly.
- `actionsExecuted` — migrations actually dispatched. From `Executor.getActionsExecuted()`.
- `conversionRate` — `actionsExecuted / actionableCycles * 100`. Primary discriminating metric.

### Interface amendments
- `Analyser` — `getActionableCycles()`. Incremented when at least one OVERLOADED **or UNDERLOADED** state present in output array.
- `Executor` — `getActionsExecuted()`. Incremented once per non-sentinel `execute()` call.

### Validated runs
- **3×3×3×1 (27 permutations)** — counter accumulation bug identified and fixed via reflection instantiation.
- **3×3×3×3 (81 permutations)** — executor slot confirmed non-discriminating. Conversion rate is the primary signal. makespan non-discriminating across all runs.

---

## Next Steps

- **Scale Monitor and Analyser to 5 variants** — update specs with finalised GUID ruleset, regenerate. Target: 5×5×3×3 = 225 permutations minimum.
- **Finalise specs** — Monitor and Analyser specs need GUID ruleset, updated approved API, and `hasOverloaded || hasUnderloaded` counter definition before next generation run.
- **VM scaling — scale-in** — deferred.
- **Energy/power loop** — deferred.

---

## Known Limitations

- RAM and BW signals are static allocations in CloudSim — they don't fluctuate dynamically. Excluded from active GUID family.
- `host.getAvailableMips()` does not exist in this CloudSim build. CPU utilisation must be computed by iterating `host.getGuestList()` and summing `vm.getTotalUtilizationOfCpuMips(now)`.
- `ReadSpace` exposes CloudSim entity objects — modules can call arbitrary CloudSim APIs on them. Genuine confinement would require entity wrapper types. Not worth the generation space cost.
- Single datacenter is a deliberate constraint for the generation experiment, not a framework limitation.
- Executor slot has minimal variation space under the current `int[]{vmId, hostId}` contract. All correctly generated executors are functionally equivalent.
