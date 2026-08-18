# Project Handoff

## Goal

Test whether LLMs can be used to rapidly prototype code modules that compose into functional autonomous network controllers. The approach:

1. Hand-code reference implementations of each MAPE module type
2. Use LLM prompts to generate variant modules against the same interfaces
3. Build a permutation constructor that tests all valid module combinations
4. Characterise which permutations produce functional controllers, and how failures manifest

The central question is **feasibility**: can LLM-generated modules compile, integrate, and produce meaningful closed-loop control behaviour without manual correction? GUID composability is a secondary mechanism used to label and characterise module compatibility — not the primary hypothesis.

---

## Architecture

### Core Framework

- **`HollowedControl`** — generic broker extending `DatacenterBroker`, wires the MAPE pipeline together. Takes Monitor, Analyser, Planner, Executor as constructor arguments. Fires an observation cycle on a fixed `observationRate`. Implements `ActionSpace`.
- **`ReadSpace`** — read-only interface exposing simulation state. Monitor, Analyser, and Planner receive this.
- **`ActionSpace`** — extends `ReadSpace`. Adds write methods. Executor receives this. `HollowedControl` implements `ActionSpace`, satisfying both interfaces.
- **`LoadState`** — enum: `OVERLOADED`, `UNDERLOADED`, `BALANCED`.

### Module Interfaces

```
Monitor      observe(ReadSpace)                    -> double[]
Analyser     analyse(double[], ReadSpace)          -> LoadState[]
Planner      plan(LoadState[], ReadSpace)          -> int[]
Executor     execute(int[], ActionSpace)           -> boolean
```

### Interface Definitions

```java
public interface ReadSpace {
    List<HostEntity> getAllHosts();
    List<GuestEntity> getVmList();
    double getNow();
    Integer getDatacenterFor(GuestEntity vm);
    Integer getUserId();
}

public interface ActionSpace extends ReadSpace {
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    void requestVmCreation(GuestEntity vm, int targetDatacenter);
    void sendCloudlet(int datacenterId, Cloudlet cloudlet);
    void moveCloudlet(Cloudlet cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId);
}
```

### Data Flow

- **`double[]`** — flat array of metric values. Index i corresponds to the i-th host in `readSpace.getAllHosts()`. Positional ordering is stable within a simulation run.
- **`LoadState[]`** — flat array of classification verdicts. Same positional ordering as the metric array.
- **`int[]`** — a single migration pair `[vmId, targetHostId]`. One migration per observation cycle. Sentinel value `{-1, -1}` signals no action.

### Key Design Decisions

- **`ReadSpace` / `ActionSpace` split** — enforced by the type system. Monitor receives `ReadSpace`, structurally prevented from writing. Executor receives full `ActionSpace`.
- **Single datacenter** — deliberate constraint for the generation experiment. Simplifies positional indexing and migration. Multi-datacenter support exists in the framework but is out of scope.
- **Flat primitive arrays** — removes CloudSim entity objects from inter-module boundaries, reduces LLM prompt complexity. Composability is purely a matter of GUID matching.
- **One migration per cycle** — Planner returns a single `int[]` pair. Firing multiple migrations before the next observation resolves would act on stale state.
- **Framework is MAPE, not MAPE-K** — stateless per cycle. Flagged as future work.
- **Boundary leak acknowledged** — `ReadSpace` exposes CloudSim entity objects. Modules can call arbitrary CloudSim APIs on them. Genuine confinement would require wrapper types — not worth the generation space cost. Flagged in thesis.

---

## GUID Convention

GUIDs are a labelling mechanism, not a runtime enforcement mechanism. Their purpose is to ask two empirical questions:
1. Can the LLM correctly self-label a module's function?
2. When all labels in a pipeline match, is the controller actually coherent?

The permutation constructor checks GUID compatibility per combination but does not gate execution. Incompatible combinations still run — characterising how they fail is part of the experiment.

### GUID Naming Ruleset

#### Monitor output GUID

Format: `host-<metric>-<type>`

| Segment | Options |
|---|---|
| `metric` | `cpu`, `vm` |
| `type` | `util` (0–1), `demand` (may exceed 1), `count` (raw integer) |

Examples: `host-cpu-util`, `host-cpu-demand`, `host-vm-count`

#### Analyser input / output GUIDs

- **inputGuid** — must match a monitor outputGuid: `host-<metric>-<type>`
- **outputGuid** — takes the form `host-<metric>-<type>-loadstate`, preserving the metric and type from the input. Example: a CPU utilisation analyser declares `inputGuid: "host-cpu-util"`, `outputGuid: "host-cpu-util-loadstate"`.

The metric-specific output GUID encodes what the `LoadState[]` array was computed from. A `LoadState` array derived from CPU utilisation carries different semantics to one derived from VM count, even though both are `LoadState[]` at the Java type level.

#### Planner input / output GUIDs

- **inputGuid** — must match an analyser outputGuid: `host-<metric>-<type>-loadstate`
- **outputGuid** — fixed: `host-migration-pair`

The planner must use its inputGuid to guide its internal logic. If `inputGuid` is `host-vm-count-loadstate`, the planning strategy and any VM scoring must operate on VM counts, not MIPS values. The GUID is an active semantic contract on planning behaviour, not just a routing key.

#### Executor input GUID

- **inputGuid** — fixed: `host-migration-pair`

#### Full pipeline chain

```
Monitor       outputGuid: "host-<metric>-<type>"
                  |
Analyser      inputGuid:  "host-<metric>-<type>"
              outputGuid: "host-<metric>-<type>-loadstate"
                  |
Planner       inputGuid:  "host-<metric>-<type>-loadstate"
              outputGuid: "host-migration-pair"
                  |
Executor      inputGuid:  "host-migration-pair"
```

Compatibility check: `monitor.outputGuid() == analyser.inputGuid()`, `analyser.outputGuid() == planner.inputGuid()`, `planner.outputGuid() == executor.inputGuid()`.

### GUID Encodes the Data Contract

An analyser declaring `inputGuid: "host-cpu-util"` may safely use [0,1] thresholds. One declaring `inputGuid: "host-vm-count"` knows it receives raw integer counts and must calibrate accordingly.

**`LoadState` semantic ambiguity:** `LoadState.UNDERLOADED` does not carry a guaranteed meaning about provisioned capacity — it only reflects the metric on which classification was based. A host classified UNDERLOADED via CPU utilisation may still be provisioned to full capacity and reject migrations via `isSuitableForGuest`. This ambiguity is not detectable by GUID matching and is documented as a thesis finding.

---

## Prompting Approach

Two-file prompting structure per generation run:

- **`0. System Context.md`** — persistent. Role, architecture, interfaces (`ReadSpace`, `ActionSpace`, `observeAndAct` loop), constraints (ASCII only, approved API only, one file per variant, zip deliverable).
- **`1–4. <Type> Specification.md`** — per module type. Specifies: module type, variant count, data structures, GUID naming convention, module interface, import stub, approved CloudSim API, and `INITIATE GENERATION` trigger.

The LLM is expected to assemble GUIDs correctly from the naming convention — GUIDs are not pre-filled. GUID self-assembly is part of what is being tested.

### Approved CloudSim API (VM Migration Family)

**Monitor** — `host.getTotalMips()`, `host.getGuestList()`, `host.getId()`, `vm.getId()`, `vm.getTotalUtilizationOfCpuMips(double time)`, `vm.getCurrentRequestedTotalMips()`

**Analyser** — None (operates on the `double[]` array only)

**Planner** — `vm.getCurrentRequestedTotalMips()`, `vm.getId()`, `host.isSuitableForGuest(vm)`, `host.getId()`, `host.getGuestList()`

**Executor** — None (operates on `int[]` and calls `ActionSpace` methods only)

---

## Completed Reference Modules

### Control Loop 1 — Cloudlet Migration (CPU/ETC)
- **Monitor1** — measures ETC per VM.
- **Analyser1** — classifies VMs using mean ± stddev on ETC.
- **Planner1** — identifies most/least loaded VMs, evaluates migration worthwhileness via MIPS-based estimate, selects largest cloudlet from overloaded VM.
- **Executor1** — calls `moveCloudlet(...)` which sends native `CLOUDLET_MOVE` event.

### Control Loop 2 — VM Migration (host-level, single metric)
- **Monitor6** — iterates `readSpace.getAllHosts()`, computes `cpu_util = used_mips / total_mips`. Returns `double[]`. `outputGuid: "host-cpu-util"`.
- **Analyser4** — fixed thresholds UPPER=0.8, LOWER=0.2. `inputGuid: "host-cpu-util"`, `outputGuid: "host-cpu-util-loadstate"`.
- **Planner4** — finds first overloaded host, selects highest-MIPS VM, checks `host.isSuitableForGuest(vm)`, prefers UNDERLOADED destination, falls back to BALANCED. Returns `int[]{vmId, targetHostId}` or `{-1,-1}`. `inputGuid: "host-cpu-util-loadstate"`, `outputGuid: "host-migration-pair"`.
- **Executor4** — looks up VM and host by ID, calls `actionSpace.requestVmMigration(vm, targetHost)`. `inputGuid: "host-migration-pair"`.

### Control Loop 3 — VM Scaling (CPU Utilisation)
- **Monitor2** — reused from Control Loop 2.
- **Analyser2** — reused, UPPER=0.6.
- **Planner3** — if overloaded hosts outnumber underloaded hosts, emits CreateVmAction. Scale-in not yet implemented.
- **Executor3** — checks `isVmCreationPending()`, constructs new `Vm` with hardcoded specs, calls `requestVmCreation(datacenterId)`.

#### Key CloudSim VM scaling notes
- `DatacenterBroker` does not natively support mid-simulation VM creation. `HollowedControl` extends this by adding new VMs to `getGuestList()` before sending `VM_CREATE`, so `processVmCreateAck` correctly populates `vmsToDatacentersMap`.
- A `vmCreationPending` flag (cleared in `processVmCreateAck`) prevents duplicate creation requests within the same cycle.

---

## LLM-Generated Modules — VM Migration Family (5×5×5×3)

### Current Variants

**Monitors** (outputGuid in parentheses)
- **monitor_v1** — CPU utilisation. `used_mips / total_mips`. (`host-cpu-util`)
- **monitor_v2** — CPU demand pressure. `sum(vm.getCurrentRequestedTotalMips()) / host.getTotalMips()`, clamped to 1.0. (`host-cpu-demand`)
- **monitor_v3** — VM count. Raw count of resident VMs per host. (`host-vm-count`)
- **monitor_v4** — Novel cpu-related metric. GUID and implementation to be confirmed from source.
- **monitor_v5** — CPU utilisation variance. Invented metric outside the permitted spec selection. See observations. (`host-cpu-variance` or similar)

**Analysers** (inputGuid → outputGuid)
- **analyser_v1** — Fixed thresholds (UPPER=0.8, LOWER=0.2). (`host-cpu-util` → `host-cpu-util-loadstate`)
- **analyser_v2** — Statistical (mean ± stddev) with absolute floor. (`host-cpu-util` → `host-cpu-util-loadstate`)
- **analyser_v3** — Statistical (mean ± stddev), wider sensitivity. (GUID to be confirmed from source)
- **analyser_v4** — IQR outlier detection. (`host-cpu-demand` → `host-cpu-demand-loadstate`)
- **analyser_v5** — Mean-relative thresholds (OVERLOADED if value > mean×1.5, UNDERLOADED if value < mean×0.5). (`host-vm-count` → `host-vm-count-loadstate`)

**Planners** (inputGuid → `host-migration-pair`)
- **planner_v1** — Overload relief. Triggers on OVERLOADED. Selects highest-MIPS VM, prefers UNDERLOADED destination, falls back to BALANCED.
- **planner_v2** — Consolidation. Triggers on UNDERLOADED. Selects lowest-MIPS VM, prefers BALANCED destination.
- **planner_v3** — Overload relief variant.
- **planner_v4** — Variant. Implementation to be confirmed from source.
- **planner_v5** — Low conversion rate (9–17% on active pairings), but produces the single best load-balance result in the dataset under the corrected demand-based ground truth (0.021875, a 90.2% reduction vs. baseline). Originally characterised as a "systematic failure" under the deprecated cpu-util ground truth — that verdict was an artifact of ground-truth noise, not a flaw in planner logic. See "Demand-Based Ground Truth Run" and "Known Limitations" below.

**Executors** (inputGuid: `host-migration-pair`)
- **executor_v1, v2, v3** — All functionally equivalent. Resolve VM and host by ID, call `requestVmMigration()`. Confirmed non-discriminating across all 375 permutations.

### 5×5×5×3 Permutation Results Summary — CPU-Util Ground Truth (SUPERSEDED 2026-07-01)

**Superseded.** This run used the deprecated `getTotalUtilizationOfCpuMips`-based ground truth, later found to be scheduler-timing-noise-dependent and structurally uncapped (see "CloudSim Observability Constraint" and "Ground Truth Metric" below). Retained for historical comparison — most qualitative findings replicated under the corrected demand-based metric (see "5×5×5×3 Run — Demand-Based Ground Truth (Current)" below), with one notable reversal (planner_v5).

375 total permutations. All completed without crash (FAILED status: 0).

**Baseline (no controller, `MeasuringBroker`, same scenario): 2.687.**

22 distinct variance values across 375 permutations. Range: 0.665 to 2.687 (spread = 2.02).

| Tier | variance | vs baseline | controllers |
|---|---|---|---|
| Baseline (no controller) | 2.687 | — | — |
| Dead controllers (analyser_v2/v4) | 2.687 | 0% | 120 |
| Compatible (worst) | 2.687 | 0% | — |
| Compatible (best) | 0.707 | −74% | monitor_v3+analyser_v5+planner_v3 |
| Incompatible (best) | 0.665 | −75% | monitor_v1+analyser_v3+planner_v4 |

**The planner slot is now the primary discriminating component.** Within `monitor_v1 + analyser_v1`, planner choice alone spans a variance range of 0.95 (0.670 to 1.617) while all planners have similar conversion rates (9–36%). Migration strategy quality matters far more than migration frequency.

| Analyser | Variance range | Note |
|---|---|---|
| analyser_v1 | 0.670–1.617 | Fixed thresholds — active; wide planner-driven spread |
| analyser_v2 | 2.687 (always) | Dead signal in this scenario — 0% conversion, baseline-level variance |
| analyser_v3 | 0.665–1.061 | Statistical thresholds — effective, narrower spread than v1 |
| analyser_v4 | 2.687 (always) | IQR collapses — zero migrations, baseline-level variance |
| analyser_v5 | 0.665–1.133 | Mean-relative — effective regardless of monitor GUID pairing |

**Conversion rate is negatively correlated with quality at the planner level.** planner_v2 (consolidation, triggers on UNDERLOADED) achieves 36% conversion but produces the worst load balance among active planners (1.617). planner_v4 achieves 13% but produces the best (0.670). Fewer well-chosen migrations outperform frequent poor ones. The consolidation strategy actively worsens balance by moving VMs in the wrong direction.

**GUID compatibility does not predict performance.** Compatible mean variance = 1.934; Incompatible mean = 1.695. Compatible combinations include both the best (0.707) and worst (2.687) outcomes — the analyser's internal logic determines quality, not its GUID label.

**analyser_v2 and analyser_v4 are functionally equivalent to no controller** — their variance matches the unmanaged baseline exactly.

**makespan**: non-discriminating — identical across all 375 permutations.

**Status breakdown** (125 unique controller configurations): ACTIVE 89, INERT 10.

### Key Observations from 5×5×5×3 Run — CPU-Util Ground Truth (SUPERSEDED 2026-07-01)

**Superseded — see caveat above.** One finding below (planner_v5) was overturned by the demand-based re-run; the rest replicated. Kept intact for the record.

**Structural compliance: 100%** — all modules compiled and integrated without modification.

**GUID self-labelling is accurate.** Modules that operate on a novel or non-standard metric correctly declared non-standard GUIDs rather than falsely claiming a standard one. analyser_v4 labelled its output `host-cpu-demand-loadstate`; analyser_v5 labelled `host-vm-count-loadstate`; monitor_v5 declared a novel GUID for its invented metric. The LLM's self-labelling behaviour is honest even when it breaks compatibility.

**monitor_v5 invented a metric outside the spec** — CPU utilisation variance is not in the permitted metric selection (cpu-util, cpu-demand, vm-count). It is a valid and meaningful metric computed using only approved API calls. This is *metric invention*, not API hallucination: structurally valid, semantically coherent, outside the specification boundary.

**analyser_v4 (IQR on cpu-demand) — dead signal.** IQR outlier detection requires variance to find outliers. CPU demand values in the current scenario cluster tightly, causing the IQR to collapse and fences to converge. Only 3–7 actionable cycles detected across all monitor pairings, 0 conversion in all runs. IQR is statistically rigorous but environmentally brittle in a low-variance, small-host-count scenario.

**analyser_v5 (mean-relative on vm-count) — dominant performer despite GUID incompatibility.** Mean×1.5/0.5 thresholds are distribution-agnostic: they adapt to whatever scale they receive. When paired with cpu-util monitors, the analyser calibrates to [0,1] values and effectively becomes a relative CPU threshold. This produces the highest conversion rates in the dataset (50–89%), all from GUID-incompatible combinations. The LLM generated a policy so generic it transcends the GUID contract it was supposed to implement.

**GUID compatibility does not predict performance.** Compatible combinations reliably filter INERT pipelines (no compatible combination is INERT), but do not predict quality within the ACTIVE set. The best-performing controllers are all GUID-incompatible.

**`LoadState` is an underspecified contract.** `UNDERLOADED` means the host is below the analyser's load threshold for the monitored metric. It does not imply spare provisioned capacity. `isSuitableForGuest` checks provisioned capacity, not runtime utilisation — a host correctly classified UNDERLOADED can still reject every migration attempt. This semantic gap is not detectable by GUID matching.

**`isSuitableForGuest` misused as selection criterion.** LLM-generated planners use it as the primary destination selection criterion rather than as a final feasibility guard. Correct pattern: (1) select destination on utilisation grounds using the metric indicated by inputGuid, (2) call `isSuitableForGuest` as a final guard, (3) iterate candidates on rejection rather than returning sentinel. The approved API list includes `isSuitableForGuest` but does not specify its intended role — this is a prompting gap.

**planner_v5 — systematic failure (OVERTURNED, see Demand-Based Ground Truth Run below).** 15% conversion rate, avg 9% when it does convert. Under the cpu-util ground truth this looked like a failure to find valid migration targets. Under the corrected demand-based ground truth, the same low-conversion behaviour produces the best variance in the dataset — the "failure" label was measuring ground-truth noise, not planner quality.

**monitor_v2 consistently underperforms.** Best achievable variance with monitor_v2 is 0.823 (paired with analyser_v5) vs 0.665 with other monitors. Despite measuring the same underlying signal as monitor_v1 via a different API call, its values lead to worse planner decisions. See CloudSim Observability Constraint below.

**monitor_v3 (vm-count) — active but mid-tier performance.** Achieves 0.707 when paired with compatible analyser_v5+planner_v3. VM count is a coarser signal than demand pressure but still actionable in an undersubscribed scenario where host occupancy correlates with load.

**planner_v2 (consolidation) is actively harmful.** Triggers on UNDERLOADED hosts and moves VMs away from them, which redistributes load to already-busy hosts. In an undersubscribed scenario, this worsens rather than improves balance. Highest conversion rate (up to 36%) but worst load balance outcome among active planners.

**Executor slot confirmed non-discriminating** across all 375 permutations.

**makespan non-discriminating** — identical across all permutations. VM migration redistributes CPU load but cannot create MIPS. In a time-shared scheduler, total work-time is conserved regardless of migration decisions.

---

### 5×5×5×3 Run — Demand-Based Ground Truth (CURRENT, 2026-07-01)

Same 375-permutation sweep (5 monitors × 5 analysers × 5 planners × 3 executors), re-run with the ground truth switched from `getTotalUtilizationOfCpuMips` to `getCurrentRequestedTotalMips` (demand pressure) — see "Ground Truth Metric" and "CloudSim Observability Constraint" below for why this switch was made and why it is structurally sound.

**Baseline (no controller): 0.222222.** This is not just an empirically low number — it is the exact value every dead/INERT pipeline converges to, confirming it is the true unmanaged floor for this scenario (same convergence property held under the old metric).

**Overall best: 0.021875** (`monitor_v1 + analyser_v1 + planner_v5`) — a **90.2% reduction** vs. baseline, well beyond the best result under the old metric (74%). 32 distinct variance values across 375 permutations; range 0.021875–0.222222.

125 unique (monitor, analyser, planner) triples: 115 ACTIVE, 10 INERT (× 3 non-discriminating executors = 375 rows). The same 10 triples are INERT as in the util-based run — `{monitor_v2, monitor_v3} × analyser_v4 × {all planners}`.

| Tier | variance | vs baseline | controllers |
|---|---|---|---|
| Baseline (no controller / INERT) | 0.222222 | — | 10 unique triples |
| Dead-but-ACTIVE (analyser_v2 always; analyser_v4 w/ monitor_v1/v4/v5) | 0.222222 | 0% | 0% conversion in all cases |
| Best | 0.021875 | −90.2% | monitor_v1+analyser_v1+planner_v5 |
| Second tier | 0.024537 | −89.0% | several pairings, mostly with planner_v5 |

**Replicated findings (unchanged from the cpu-util run):**

- **analyser_v2 and analyser_v4 are still dead signal.** analyser_v2 flags actionable cycles (45/45) but converts 0% of them in every single pairing, landing exactly on baseline. analyser_v4 (IQR) is INERT (0 actionable cycles) when paired with monitor_v2/monitor_v3, and ACTIVE-but-0%-conversion with the other three monitors. Same root cause as before (IQR collapses on low-variance input).
- **analyser_v5 (mean-relative) is the dominant analyser**, and more clearly so now: mean variance 0.053 — less than half of the next-best analyser (analyser_v1 at 0.112) — and it never bottoms out at baseline (max 0.126, i.e. no dead combination exists for it).
- **planner_v2 (consolidation) is still the worst active planner** — highest mean variance among active planners (0.098 on converting runs) and the highest average conversion rate (37.6%). Confirms "frequent migrations in the wrong direction beat nothing, but underperform selective correct ones."
- **GUID compatibility still does not predict quality.** Compatible-combination mean (0.155) is slightly worse than incompatible-combination mean (0.146) — same direction as the cpu-util run.
- **Executor and makespan remain non-discriminating.**
- **Conversion rate vs. quality, restated more precisely:** across converting runs, mean variance ranks with conversion rate almost in lockstep — planner_v5 (17% conversion → 0.032 variance), planner_v3 (32% → 0.058), planner_v4 (33% → 0.068), planner_v1 (28% → 0.074), planner_v2 (38% → 0.098). Fewer, well-targeted migrations consistently outperform frequent ones.

**Reversed finding — the one to flag prominently in the thesis:**

**planner_v5 flips from "systematic failure" to best-in-dataset.** Its conversion rate is unchanged (9–17%, same low-activity profile as before) — what changed is the ground truth used to judge the outcome. Under the noisy cpu-util metric, its results looked mediocre-to-bad; under the demand-based metric, the same decisions produce the lowest variance in the entire sweep. This is a concrete, dataset-level demonstration that a module-quality conclusion can be an artifact of ground-truth measurement error rather than the module's actual logic — worth its own paragraph in the methodology/limitations discussion, independent of the GUID-composability findings.

**Practical implication:** any per-module characterisation made under the old ground truth (monitor_v2 underperformance, planner rankings, etc.) should be treated as provisional until cross-checked against the demand-based numbers. Everything except planner_v5 held up under the switch, which is reassuring for the framework's overall validity, but the planner_v5 case shows this cross-check is necessary, not optional.

### planner_v5 — Mechanism Confirmed from Source (2026-07-01)

Reading `planner_v5.java` resolved *why* the reversal happens, and it's a better explanation than "ground-truth noise" alone.

`planner_v5` implements **global variance minimisation via exhaustive search**: it snapshots every host's load as `Σ vm.getCurrentRequestedTotalMips()` (demand pressure, not cpu-util), simulates every candidate (OVERLOADED-source, UNDERLOADED-destination, suitable) migration, and only commits to the one that strictly reduces that variance versus doing nothing. Two consequences:

- **It only uses the `LoadState[] diagnosis` array as a coarse gate** (source must be OVERLOADED, destination must be UNDERLOADED) — the actual scoring is recomputed independently from raw `ReadSpace` entity state, via the `getCurrentRequestedTotalMips()` call already on the approved Planner API list. This is legitimate use of the existing boundary leak (`ReadSpace` exposes CloudSim entities — see Key Design Decisions), not a hallucination or an unapproved call.
- **Its internal objective function and the demand-based ground truth are essentially the same quantity.** Both are population variance of per-host demand pressure; since all 6 hosts in this scenario are homogeneous (identical MIPS capacity), variance of raw requested-MIPS load and variance of the capacity-normalised ratio rank identically. planner_v5 isn't winning because it "got smarter" under the new metric — it's winning because the evaluation metric now finally measures the same thing it was always internally optimising for. Under the old cpu-util ground truth, it was being scored on a quantity it never used internally, which is why it looked mediocre.

This also explains why analyser choice barely matters for `monitor_v1 + planner_v5` (byte-identical results across analyser_v1/v3/v5, see below): those three all agree almost every cycle on which hosts are OVERLOADED/UNDERLOADED (monitor_v1's readings are bimodal — near-0 when idle, far above any threshold when active — see CloudSim Observability Constraint), so the candidate pool planner_v5 searches is nearly identical regardless of which analyser produced it, and its own scoring picks the same migration from that pool every time.

**Correction to the GUID self-labelling narrative:** planner_v5 declares `inputGuid: "host-cpu-util-loadstate"` but its scoring logic is entirely demand-based, independent of whatever metric produced the LoadState array. This is not dishonesty about what data it receives (it genuinely does gate on the declared LoadState array) — but it does violate this project's own stated rule that "the GUID is an active semantic contract on planning behaviour, not just a routing key" (see GUID Naming Ruleset). The better framing, on reflection, isn't "mislabelling" — it's that planner_v5 is **GUID-agnostic by design**, structurally the same pattern as analyser_v5 being distribution-agnostic (see below): both of the two best-performing modules in the entire sweep are the ones that decline to let their declared input contract constrain their internal logic, and instead verify against something more universal. That looks like a genuine, generalisable finding: GUID-agnostic modules outperformed GUID-calibrated ones at two independent pipeline stages.

**Caveat:** this is a small-sample result — only 5–6 migrations total across 45 cycles for the best pairings — and monitor_v1's coarse-gate viability is likely scenario-dependent (see "Actionable-Cycle Gate" below and CloudSim Observability Constraint). Worth a seed/VM-mix variation before treating monitor_v1+planner_v5 as a canonical best configuration rather than a favourable alignment specific to this scenario instance.

---

### Actionable-Cycle Gate — Diagnosed as Weak, Instrumentation Redesigned (2026-07-01)

**Finding:** `actionable_cycles` (Analyser flags ≥1 host as OVERLOADED **or** UNDERLOADED) saturates to the ceiling (45/45, the maximum possible given `makespan=4530` / `observationRate=100`) in 66% of all 375 rows and 72% of ACTIVE rows. Breaking down by analyser:

- **analyser_v1, analyser_v2, analyser_v3** hit exactly 45/45 in *every* pairing, with *every* monitor, no exceptions — including monitor_v2/v3/v4, which are well-bounded, sane signals. This isn't downstream of cpu-util's brokenness; it's these three analyser designs (fixed / mean±stddev / floor-based single-pass thresholds) combined with a scenario deliberately engineered for persistent, not transient, imbalance (First Fit placement — see Simulation Scenario). With 6 independently-thresholded hosts and a permanently uneven initial state, "at least one host out of band" is a bar these three analysers apparently never fail to clear.
- **analyser_v4 (IQR) and analyser_v5 (mean-relative)** are the only two with real dynamic range (0–45), and analyser_v5's count visibly *drops* when a better planner is installed for the same monitor+analyser pair — genuine evidence the closed loop is resolving imbalance, not just permanently flagging it.

**Root cause of the metric's weakness:** "at least one OVERLOADED or UNDERLOADED" measures *an anomaly exists*, not *a migration is possible*. A migration needs a source (OVERLOADED) **and** a destination (UNDERLOADED) to coexist in the same cycle — most planners (`planner_v5` strictly; others via a BALANCED-destination fallback) need both, not either. The current gate can be maximally saturated while genuine source/destination co-occurrence is rare, which means part of the gap between high actionable_cycles and low conversion_rate reflects the metric's looseness, not planner selectivity — the two were being conflated.

**Resolution — a stricter, planner-agnostic "opportunity" gate**, computed purely from the same `LoadState[]` array: `opportunityCycles` requires ≥1 OVERLOADED **and** ≥1 UNDERLOADED in the same cycle. This is a materially better denominator for `conversion_rate`, since it approximates "could any reasonable planner have acted here" without needing per-planner fallback knowledge. `imbalanceCycles` (the old `actionable_cycles`, renamed conceptually though the CSV column name is kept for continuity) is retained as an analyser-health diagnostic only — it is not a controller-effectiveness metric, since it's degenerate (constant) for 3 of 5 analysers in this scenario.

A further, deferred option: an *oracle* "improving-opportunity" check — run planner_v5's own exhaustive search as a standalone diagnostic regardless of which planner is actually installed, giving a ceiling any of the five planners' realised conversions could be benchmarked against. Not implemented; noted as a stretch goal.

Ground truth (`groundTruthAvgVariance`) remains the top-line success measure regardless of which gate is used — these are diagnostic layers underneath it, meant to explain results, not replace them.

---

## Simulation Scenario

`ConstructorVariableVM.java` — current scenario configuration:

- **Hosts**: 6 × quad-core (4 PEs × 1000 MIPS = 4000 MIPS total per host)
- **VMs**: 12, MIPS drawn randomly from `{2000, 500, 1000}` with seed 42. Total VM demand ≈ 14,000 MIPS vs 24,000 MIPS host capacity — scenario is undersubscribed, creating genuine migration headroom on light hosts.
- **Cloudlets**: 60, lengths uniform random in [10,000, 500,000] MI with seed 42, `UtilizationModelFull`. Longer upper bound (500,000 MI) extends simulation duration, giving controllers more observation cycles to act and differentiate.
- **Scheduling**: `CloudletSchedulerTimeShared`, `VmSchedulerTimeShared`
- **Placement**: `VmAllocationPolicySimpler` (First Fit) — packs VMs onto early hosts in order, creating structural imbalance. Chosen deliberately over Worst Fit / LeastFull to create an uneven initial state that controllers can act on.
- **Observation rate**: 100 time units.

### Ground Truth Metric

**Status: RESOLVED 2026-07-01.** Ground truth has been switched from `getTotalUtilizationOfCpuMips(now)` to `getCurrentRequestedTotalMips()`. Old baseline (2.687) and the util-based 5×5×5×3 run are retained above as historical/superseded. New baseline: **0.222222**. See "5×5×5×3 Run — Demand-Based Ground Truth (Current)" above for full results.

Why the switch is justified, not just preferred — confirmed against CloudSim source (`Cloudslab/cloudsim`):

- `getTotalUtilizationOfCpuMips` = `getTotalUtilizationOfCpu(time) * getMips()`, where `getTotalUtilizationOfCpu` sums `cl.getUtilizationOfCpu(time)` over every cloudlet in the VM's exec list with no normalisation. Under `UtilizationModelFull` (fixed at 1.0/cloudlet), this is literally a count of concurrently-executing cloudlets, and `getMips()` is the VM's **per-PE** rating, not total capacity — the product has MIPS units but no relationship to host capacity. This is why observed cpu-util readings hit 2.75× host capacity (see t=100 snippet, 2026-07-01 conversation) instead of staying in [0,1] as the GUID type table promises.
- `getCurrentRequestedTotalMips()` sums `currentMipsShare`, the MIPS actually allocated by the host's `VmSchedulerTimeShared`. That scheduler's Javadoc states it explicitly: *"This scheduler does not support over-subscription."* Its `allocatePesForGuest` hard-rejects any allocation that would push the running total past the host's `availableMips` (initialised to `host.getTotalMips()`). This means **the sum of demand pressure across all VMs on a host is capped at 1.0 by construction** — not an empirical property of this undersubscribed scenario, but a scheduler invariant. This is the property you want in a ground-truth signal.

Net effect: cpu-util was both noisy (scheduler-timing-dependent, can read exactly 0.0 mid-run) and structurally unbounded (can read many multiples of 1.0), i.e. it failed in both directions. cpu-demand is bounded and deterministic. The old `host-cpu-util` GUID should be considered unreliable as a [0,1]-typed signal going forward; `host-cpu-demand` is the validated signal.

`HollowedControl` measured **average CPU demand pressure variance across hosts** (historical/deprecated implementation shown below used `getTotalUtilizationOfCpuMips`; current implementation uses `getCurrentRequestedTotalMips` in the same position), computed independently of the MAPE pipeline on every observation cycle:

```java
// Per cycle:
double[] utils = new double[hosts.size()];
double mean = 0.0;
for (int i = 0; i < hosts.size(); i++) {
    double usedMips = 0;
    for (GuestEntity vm : hosts.get(i).getGuestList()) {
        usedMips += vm.getTotalUtilizationOfCpuMips(now);
    }
    utils[i] = usedMips / hosts.get(i).getTotalMips();
    mean += utils[i];
}
mean /= hosts.size();
double variance = 0.0;
for (double u : utils) variance += (u - mean) * (u - mean);
groundTruthVarianceSum += variance / hosts.size();
groundTruthCycleCount++;

// Result exposed via:
public double getGroundTruthAvgVariance() {
    return groundTruthCycleCount == 0 ? 0.0 : groundTruthVarianceSum / groundTruthCycleCount;
}
```

**Interpretation**: population variance of per-host demand pressure ratios, averaged over the run. Lower = more balanced load distribution. Values can exceed 0.25 (the theoretical max for values in [0,1]) because demand pressure can exceed 1.0 in oversubscribed hosts — this is expected and correct.

**Why not threshold-based overloaded host-cycles?** Earlier metric (`total_overloaded`, counting host-cycles above 0.8 threshold) collapsed to zero variance across all controllers (all produced exactly 7) after scenario configuration changes. The threshold is too blunt when all hosts are near-saturated; any controller that acts at all lands at the same floor. Demand pressure variance captures the *distribution* of load rather than a binary threshold, and is sensitive to migration decisions regardless of absolute utilisation level.

**Why not clamp utils to [0,1]?** In the current undersubscribed scenario demand pressure values do stay below 1.0 on most hosts, so clamping would not collapse variance — but it is omitted for consistency and because demand pressure is the intended signal. In earlier oversubscribed configurations (24 VMs), clamping would have collapsed all values to 1.0 and zeroed variance.

**Ground truth is not independent of the monitors.** The current (demand-based) ground truth uses the same `getCurrentRequestedTotalMips()` call as monitor_v2. The deprecated util-based ground truth likewise duplicated monitor_v1's internal call. See CloudSim Observability Constraint below.

**Ground truth metric — resolved 2026-07-01.** `getTotalUtilizationOfCpuMips(now)` returned 0.0 at many hosts even when cloudlets were actively running (scheduler-timing artifact) and, per the source analysis above, could also read many multiples of 1.0 depending on cloudlet concurrency — noisy in both directions. Switched to `getCurrentRequestedTotalMips()` (demand pressure), which is scheduler-bounded to ≤1.0 by construction. Re-run complete: see "5×5×5×3 Run — Demand-Based Ground Truth (Current)" above. New baseline: 0.222222 (was 2.687 under the deprecated metric — not comparable in absolute terms, but the qualitative structure of the results transferred almost entirely, with one reversal: planner_v5, previously deemed a failure, now the best performer).

---

## Permutation Constructor

`ConstructorVariableVM.java` — iterates all module combinations, runs each through the standard simulation scenario, collects `SimulationResult` records, prints a summary after all runs complete.

### Design
- **Module registries** — `monitorDict`, `analyserDict`, `plannerDict`, `executorDict` as static (raw-typed) arrays. Add new variants here. Kept raw deliberately — see "Instrumentation Architecture" below for why raw-typing here is fine but was NOT fine downstream.
- **GUID validation** — checked per combination but does not gate execution. Incompatible combinations still run.
- **Fresh instances per run** — modules instantiated via reflection through a single generic `instantiate(Class<?>)` helper (one `@SuppressWarnings("unchecked")` chokepoint) to prevent counter accumulation across runs and to restore proper generic typing (`Monitor<double[]>`, `Analyser<double[],LoadState[]>`, `Planner<LoadState[],int[]>`, `Executor<int[]>`) after the raw-typed registry lookup.
- **`SimulationResult` record** — `monitorId`, `analyserId`, `plannerId`, `executorId`, `makespan`, `groundTruthAvgVariance`, `actionableCycles` (imbalance), `opportunityCycles`, `actionsExecuted`, `conversionRate`, `compatible` (boolean), `status` (ACTIVE / INERT).
- **Deferred logging** — `logResult` called after all simulations complete.

### SimulationResult Metrics
- `makespan` — max cloudlet finish time. `-1` signals failure. Non-discriminating in current scenario.
- `groundTruthAvgVariance` — average CPU demand pressure variance across hosts per cycle (double). Primary performance metric. Lower = better load balance.
- `actionableCycles` — cycles where Analyser detected at least one OVERLOADED **or** UNDERLOADED state. **Weak gate, analyser-health diagnostic only** — saturates to the ceiling for 3 of 5 analysers regardless of monitor. See "Actionable-Cycle Gate" above. Column name retained for CSV continuity with prior runs; conceptually this is "imbalance detected."
- `opportunityCycles` — **new (2026-07-01)**. Cycles where Analyser detected ≥1 OVERLOADED **and** ≥1 UNDERLOADED simultaneously. Planner-agnostic proxy for "a migration was actually possible." The intended denominator for a more meaningful conversion rate going forward.
- `actionsExecuted` — migrations actually dispatched. **Source changed 2026-07-01**: previously read from `Executor.getActionsExecuted()` (self-reported by the module); now read from `HollowedControl.getActionsExecuted()`, incremented directly off the `boolean success` already returned by `execute()` — no generic type inspection needed for this one, since `execute()`'s return type was never generic.
- `conversionRate` — `actionsExecuted / actionableCycles * 100`. Measures pipeline activity against the weak gate; historically treated as a selectivity/quality signal, which conflates "declined to act" with "had nothing valid to act on." A `conversionRate` against `opportunityCycles` is the more defensible version of this stat going forward — not yet computed in the CSV, straightforward to add.
- `status` — INERT if `actionableCycles == 0`; ACTIVE otherwise. Unchanged semantics — INERT still means the analyser never flagged anything at all, which is a different (and rarer) condition than `opportunityCycles == 0`.

### Interface Amendments — REMOVED 2026-07-08 (previously superseded 2026-07-01, see Instrumentation Architecture below)
- `Analyser` — `getActionableCycles()`. Incremented when at least one OVERLOADED **or** UNDERLOADED present in output array.
- `Executor` — `getActionsExecuted()`. Incremented once per non-sentinel `execute()` call.

**Update 2026-07-08:** these two methods, their backing counter fields, and all increment call sites have now been fully removed — from both interfaces and from all 8 affected modules (analyser_v1–v5, executor_v1–v3). This supersedes the original "leave them in place, unused" decision below: once validated end-to-end against a complete 375-row run (see "5×5×5×3 Run — Demand-Based Ground Truth" above), the self-reporting methods were confirmed to be pure vestige with no remaining reader, and — checked directly against `analyser_v1`'s source — cleanly separable from the classification/planning logic (single field, isolated increment sites, single getter), making removal mechanically low-risk. The `<Type> Specification.md` prompt documents have also been updated to drop the self-tracking requirement, so future generation batches won't be asked to implement it.

**Confirmed 2026-07-08:** full 375-permutation re-run against the trimmed modules reproduces the existing dataset exactly (same baseline 0.222222, same best 0.021875, same ACTIVE/INERT split). Removing the self-tracking bookkeeping was purely cosmetic, as expected — no classification or planning behaviour changed. The trimmed modules are now the canonical version the published results are traceable to.

Historical context (why this was originally left alone, and how it was reversed): the Analyser/Executor Java interfaces were initially left untouched specifically to avoid regeneration risk on the 18 existing modules — the reasoning below explains that original caution.

### Instrumentation Architecture (2026-07-01)

**Problem:** `getActionableCycles()`/`getActionsExecuted()` being self-reported by 18 independently LLM-generated modules meant every count in every prior analysis was trusting each module's own bookkeeping, never independently verified. Diagnosing the actionable-cycle saturation (above) was the trigger for fixing this.

**Decision:** move all three counters (`imbalanceCycles`/old `actionableCycles`, new `opportunityCycles`, `actionsExecuted`) into `HollowedControl`, computed once by hand-authored framework code, the same way for every permutation — removing an entire class of unverified measurement risk, not just tidying code.

**Generic-type obstacle:** `HollowedControl<M,D,A>` is generic, so it can't statically know `D = LoadState[]` to inspect co-occurrence, and `execute()`'s `boolean` return needed no such inspection (concrete type, not generic). Resolved by injecting two `Predicate<D>` hooks at construction time (Option B: push the type-specific logic to the boundary where the type is concretely known), defined in `ConstructorVariableVM` where `D` is bound to `LoadState[]`:

```java
// HollowedControl<M,D,A> — stays fully generic, never mentions LoadState
private final Predicate<D> imbalancePredicate;
private final Predicate<D> opportunityPredicate;
private int imbalanceCycles = 0;
private int opportunityCycles = 0;
private int actionsExecuted = 0;

// inside observeAndAct(), after analyser.analyse(...):
if (imbalancePredicate.test(diagnosis))   imbalanceCycles++;
if (opportunityPredicate.test(diagnosis)) opportunityCycles++;
...
boolean success = executor.execute(actions, this);
if (success) actionsExecuted++;   // success already IS "non-sentinel action" — no generic inspection of A needed
```

```java
// ConstructorVariableVM.java — the only place LoadState is mentioned in this wiring
diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) || hasAny(diagnosis, LoadState.UNDERLOADED),   // imbalance
diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) && hasAny(diagnosis, LoadState.UNDERLOADED)     // opportunity
```

**Reflection/generics interaction:** passing raw-typed reflection results (`monitorDict` etc. are raw arrays) directly into `HollowedControl<>`'s generic constructor poisons the whole call to raw type inference (a standard Java gotcha — once one argument is raw, the compiler can't verify the rest, and the whole invocation, including the lambda parameter types, collapses to `Object`). Fixed with a single generic factory method as the one unchecked-cast chokepoint:

```java
@SuppressWarnings("unchecked")
private static <T> T instantiate(Class<?> clazz) throws Exception {
    return (T) clazz.getDeclaredConstructor().newInstance();
}
// call sites use target-typing to infer T, e.g.:
Monitor<double[]> mFresh = instantiate(m.getClass());
```
This is safe not because the annotation makes it safe, but because each module already declares its concrete generic parameterisation at its own class definition (`class monitor_v1 implements Monitor<double[]>`), and the registries only ever contain classes validated against this family's interfaces (100% structural compliance across all runs so far) — the cast recovers information reflection's API can't preserve through erasure, it doesn't invent a new guarantee.

**Backward compatibility / future generation specs — originally proposed 2026-07-01, actioned 2026-07-08:** the Analyser/Executor Java interfaces were initially left untouched to avoid any regeneration risk on the 18 existing modules, with a deprecated-`default`-method conversion floated as a lower-risk middle ground. In the end the full removal was done instead (see "Interface Amendments" above) — both interface methods, their backing fields, and all call sites are gone from all 8 modules, and the `<Type> Specification.md` prompt documents have been trimmed to match, so future generation batches won't be asked to implement this bookkeeping at all. Pending: confirmation re-run against the trimmed modules (see above).

### Validated Runs
- **3×3×3×1 (27 permutations)** — counter accumulation bug identified and fixed via reflection instantiation.
- **3×3×3×3 (81 permutations)** — executor slot confirmed non-discriminating. makespan confirmed non-discriminating.
- **5×5×5×3 (375 permutations)** — current validated scale. Results summarised above.

### Baseline Benchmark

`MeasuringBroker` — a minimal `DatacenterBroker` subclass that runs the same scenario with no MAPE pipeline, measuring `groundTruthAvgVariance` on the same 100-time-unit schedule. Run in `ManualControllerVmMigrationSimpleCompare.java`.

**Current baseline (demand-based ground truth, 2026-07-01): 0.222222.** Confirmed as the exact convergence value of every INERT/dead pipeline in the demand-based 5×5×5×3 run — see above.

**Historical baseline (util-based ground truth, superseded): 2.687** — the natural variance reading under First Fit placement with no controller intervention, under the now-deprecated `getTotalUtilizationOfCpuMips` metric. Not comparable in absolute terms to the current baseline (different metric, different units) — retained for reference only.

Critical: both simulations must use identical scenario parameters (VMs, cloudlets, allocation policy, seeds). Cloudlet `maxLength` must be `500000` in both files — earlier runs with `100000` in `ConstructorVariableVM` produced non-comparable results. This constraint applies equally to the demand-based re-run.

---

## Cloudlet Migration Family — Second Control Loop Generation (2026-07-02–03)

### Rationale

Three candidate systems goals were identified for future controller generation: minimise QoS/SLA violations via VM migration, minimise makespan via cloudlet migration, minimise power use via VM migration/scaling. Cloudlet migration was chosen as the next target — a hand-coded reference module family (Monitor1/Analyser1/Planner1/Executor1) already exists for it, unlike the other two goals.

**Design goal: a mixed pool from the outset**, not "build in isolation, merge later." The target permutation constructor holds registries from both the VM-migration and cloudlet-migration families simultaneously, extending the existing "GUID compatibility does not predict performance" line of inquiry to cross-family pairs — not just within-metric mismatches (e.g. `analyser_v5` misreading its input scale) but across entirely different action types and indexing domains (host-indexed vs VM-indexed arrays, 2-element vs 3-element action tuples).

Generation specs remain **per-family and single-purpose** regardless — a shared prompt across both goals risks conflating two different domains in one LLM context. Only the registries and permutation constructor merge; GUID compatibility checking (already non-gating) extends naturally to cross-family pairs without further design work.

### GUID Naming Convention — Generalised

- **Monitor/Analyser layer stays an *observation-level* marker**: `host-<metric>` / `vm-<metric>` (Monitor), `<level>-<metric>-loadstate` (Analyser) — same convention as VM migration, extended with a `vm-` level for VM-indexed metrics.
- **Planner/Executor's fixed terminal GUID was reinterpreted to name the *subject of the migration action*, not the observation level.** The original VM-migration GUID, `host-migration-pair`, conflated two readings that happened to coincide for that family: "continuation of the observation-level prefix" and "type of thing that moves." Cloudlet migration breaks the coincidence — its pipeline observes at the VM level throughout, so a level-continuation reading would also yield `vm-migration`, yet the natural label for "a cloudlet is being relocated" is `cloudlet-migration`. Resolved by choosing **subject-of-migration naming** deliberately: the terminal GUID names what moves, not what it lands on or what was observed. Applied symmetrically — VM-migration's fixed GUID renamed `host-migration-pair` → **`vm-migration`** (a VM is what moves); cloudlet-migration's is **`cloudlet-migration`**.
  - Does not affect GUID compatibility checking (always direct string equality per stage boundary) or the planned cross-family "level" derivation for the mixed pool (always anchored to the Monitor/Analyser prefix, never the terminal action GUID).
  - Documented tension, not a bug: `host`/`vm` now do two independent jobs depending on pipeline position — observation-level marker at Monitor→Analyser, migration-subject marker at Planner→Executor. VM-migration's own pipeline now reads `host → host → vm`, correct but easy to misread cold.
- **Plurality segment (`-single`/`-multi`) deliberately left out of the convention.** Every current module produces exactly one action per cycle in both families (differing in tuple length, not cardinality), so a constant token adds no discriminating information. `Executor1`'s original hand-coded reference (`List<MigrationPair>`) was genuinely plural before being simplified to a single-tuple contract for this generation family — revisit only if multi-action generation is actually implemented (see Known Limitations).

### Metric Vocabulary — `util` and `demand` Both Retired

- **`util`** (cpu-util) stays retired from the active vocabulary (see the earlier Ground Truth Metric section) — noisy and structurally unbounded, though `monitor_v1` remains the best-performing dataset entry via `planner_v5`'s GUID-agnostic re-scoring exploiting its bimodal signal, not because `util` itself is a coherent stat. Not regenerated or extended.
- **`demand`** (`vm.getCurrentRequestedTotalMips() / vm.getMips()`) retired for cloudlet migration on stronger evidence than `util`'s retirement: **not noisy, a hard constant.** Confirmed via temporary diagnostic logging added to `HollowedControl.observeAndAct()` — this ratio reads exactly `1.0` for every VM, every cycle, with zero exceptions, both before and after an unrelated VM-placement bug was fixed (see below), isolating the degeneracy as a scenario-structural property rather than a confound. Root cause: `getCurrentRequestedTotalMips()` reflects the *host-granted* MIPS share (bounded by `VmSchedulerTimeShared`'s no-oversubscription guarantee), not actual cloudlet workload demand. In this deliberately-undersubscribed-host scenario, any placeable VM is unconditionally granted its full rated MIPS regardless of what its cloudlets are actually doing — so the ratio is pinned to 1.0 by construction.
  - The other three metrics (`etc`, `length`, `count`) are unaffected — all three read the VM's own cloudlet exec list directly (workload-driven), not the host-VM MIPS grant (provisioning-driven), and genuinely vary.
  - `vm.getCurrentRequestedTotalMips()` removed from the approved API for Monitor and Planner — not just because `demand` is retired, but because it's proven redundant with `vm.getMips()` in this scenario. Whether `vm.getMips()` itself still earns a place is open, contingent on whether a redefined Planner feasibility check ends up needing VM capacity information at all.
  - Any Monitor/Analyser pair still implementing `demand` as originally formulated will produce a constant, zero-variance signal, classified `INERT`/dead-signal by any downstream Analyser regardless of Planner quality.

### Reference Module Divergence — Executor1

`Executor1` (hand-coded cloudlet-migration reference) operates on `List<MigrationPair>` of live `Cloudlet`/`GuestEntity` object references, leaning fully on the `ReadSpace` boundary leak rather than solving ID-based resolution. The generated family deliberately diverges: flat `int[]{cloudletId, fromVmId, toVmId}`, matching the "keep data flow as simple as possible" principle already governing VM migration, and dropping `Executor1`'s plurality in favour of one action per cycle (see Known Limitations for the multi-action deferral this implies). `Executor1`'s `inputGuid()` also incorrectly returns `"vm-migration"` — likely a copy-paste artifact, since the module itself performs cloudlet migration; not corrected (reference-only, not part of the generation registry).

### Approved API — Findings From CloudSim Source

Confirmed by direct reading of `GuestEntity.java`, `CloudletScheduler.java`, `Cloudlet.java`, `HostEntity.java`:

- **Cloudlet lookup is VM-scheduler-scoped, not globally registered.** The only way to resolve a `Cloudlet` from a raw ID is via its hosting VM's own `CloudletScheduler`. `getCloudletExecList()` (~line 571) is the correct **non-destructive peek**; `cloudletCancel(int)` is destructive (removes the cloudlet as a side effect) and must never be used for resolution — the real removal happens exactly once, inside the Datacenter's own `processCloudletMove` handler when `CLOUDLET_MOVE` fires. Using `cloudletCancel` for an Executor-side lookup would cancel the cloudlet out from under the scheduler at send-time, causing the later legitimate cancel to return `null` and falsely mark the migration failed.
- **`Cloudlet.getCloudletLength()` is per-PE, not total.** `getCloudletTotalLength()` is the real total (`cloudletLength × numberOfPes`). Neither is needed directly: `getRemainingCloudletLength()` already computes against `getCloudletTotalLength()` internally, so it's already expressed in total-across-PEs units.
- **`getEstimatedFinishTime(Cloudlet, time)` returns an absolute simulation timestamp**, not a duration — must be used as `getEstimatedFinishTime(cl, now) - now` for `etc`.
- **Aggregation across a VM's exec list is metric-dependent, not uniform.** `length` (remaining work) is additive — sum across the exec list. `etc` (a completion time under concurrent, time-shared execution) is **not** additive — summing per-cloudlet finish times double-counts concurrency; correct aggregation is **max** (the VM finishes only once its last concurrent cloudlet finishes).
- **No cloudlet-migration equivalent of `HostEntity.isSuitableForGuest(GuestEntity)` exists.** That method answers "does this host have capacity for this VM" — a structurally different relationship from "does this VM have capacity for this cloudlet," and reusing its logic (as `planner_v1` implicitly attempted, see below) checks the wrong thing entirely. More fundamentally, `CloudletSchedulerTimeShared` has **no hard rejection condition analogous to host capacity** — a VM always accepts another cloudlet, just time-shared more thinly. Any Planner-level "adequate resources" check is answering a question the scheduler doesn't structurally enforce the way host-VM placement does.

### `ActionSpace.moveCloudlet` — Simplified to a Flat ID Contract

Original signature (matching `Executor1`'s style) took `(Cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId)`. Identified as reductive: every parameter is immediately unpacked into raw fields with no dependence on genuine object state, and the *authoritative* cloudlet resolution happens later anyway, independently, inside the Datacenter's `processCloudletMove` handler — the Executor's resolution work is computed, discarded one call later, and redone from scratch by framework code regardless.

**Resolved to `moveCloudlet(int cloudletId, int fromVmId, int toVmId)`**, matching the Planner's output shape 1:1 (implemented in `HollowedControl.java`, current state). `destDatacenterId` is now resolved internally via `getDatacenterFor(toVmId)`; `userId` via the broker's own `getUserId()` (constant across this single-user scenario). Consequence: the Executor family's job is now sentinel-check-then-pass-through only — no entity resolution, no `CloudletScheduler` access, and therefore **no destructive-cancel-vs-peek risk at all**, eliminated by construction rather than merely discouraged via spec wording. Approved CloudSim API for this Executor family is correctly `N/A` (the Executor spec's Approved API section still needs a pass to remove the stale entity-method listing left over from before this redesign — not yet done).

Executor diversity is expected to remain non-discriminating for this single-action design, consistent with the VM-migration finding ("Executor slot confirmed non-discriminating across all 375 permutations") — accepted as a known, temporary limitation pending future multi-action generation.

### Final Generation Specs

Four spec documents (`1. Monitor Specification.md`, `2. Analyser Spec.md`, `3. Planner Spec.md`, `4. Executor Spec.md`), iterated through several critique passes before generation:

- **Monitor** (5 variants): VM-level, `double[]` output, `vm-<metric>` GUID. Metrics: `etc` (max estimated finish-time duration across exec list), `length` (sum of remaining cloudlet length), `count` (exec-list size); `demand` removed post-investigation.
- **Analyser** (5 variants): VM-level, `LoadState[]` output, `vm-<metric>-loadstate` GUID, FURTHER CONTEXT enforcing "internal logic matches the type of input stated by inputGuid" (testing the `planner_v5`-style GUID-as-semantic-contract question for this family). Approved API `N/A` — operates purely on the `double[]` array.
- **Planner** (5 variants): VM-level, `cloudlet-migration` output GUID, `int[]{cloudletId, fromVmId, toVmId}` with `{-1,-1,-1}` sentinel. Approved API includes the full `GuestEntity → CloudletScheduler → Cloudlet` chain. FURTHER CONTEXT requires an explicit destination-feasibility check — **known to need revision**, see below.
- **Executor** (3 variants): flat `int[]` input, `boolean` output ("attempted", not "succeeded"), `cloudlet-migration` input GUID, no output GUID. Approved API `N/A` post-redesign.

### Debugging Session — Zero Actions Executed

First full 375-permutation cloudlet-migration sweep: **100% failure to execute any migration.** `actions_executed = 0` on every row, ground truth pinned exactly at the unmanaged baseline (0.222222) on every row — a total non-execution problem, not a quality problem. Diagnosis proceeded in stages:

1. `opportunity_cycles` nonzero in most rows (up to 42/45) — the Monitor→Analyser pipeline genuinely detects OVERLOADED/UNDERLOADED co-occurrence. The break is downstream of diagnosis.
2. No `FAILED` status anywhere, makespan normal throughout — nothing is crashing; a silent logic outcome, not an exception.
3. Uniformity across all 5 Planners × 3 Executors ruled out independent per-implementation bugs, pointing at one or two shared causes.
4. First shared cause found by direct code inspection: `HollowedControl.moveCloudlet` still required an explicit `destDatacenterId` 4th parameter the final Executor spec gave no legal way to supply. Fixed per the `moveCloudlet` redesign above.
5. Added `actionsProposed` diagnostic counter (`Predicate<A>` hook in `HollowedControl`, tested immediately after `planner.plan()`, mirroring `imbalancePredicate`/`opportunityPredicate`) to disambiguate Planner-decision failures from Executor-wiring failures. Re-run: `actionsProposed` **also** 0 across all 375 rows — confirms the failure is entirely within Planner decision logic, independent of (but not obviating) the `moveCloudlet` fix.
6. Read `planner_v1.java` directly: `hasCapacity()` implements `vm.getCurrentRequestedTotalMips()/vm.getMips() < 0.85` as its destination-feasibility check — self-contained, independent of whatever diagnosis it receives (explains the uniformity across all 5 planners: the bug is Planner-internal, not pairing-dependent). Also found `planner_v1` declares a non-standard, invented `inputGuid` (`"vm-finish-loadstate"`, not in the metric vocabulary) — a self-labelling inconsistency in the same family as `monitor_v5`'s cpu-variance invention in the VM-migration batch; doesn't gate execution (GUID mismatch never gates), so not causal, but a data point worth keeping.
7. Confirmed via temporary diagnostic logging (placed in `HollowedControl.observeAndAct()`, generic-safe, no `LoadState` reference needed) that the ratio reads exactly `1.0` for every VM, every cycle.
8. Investigated a secondary anomaly (VMs 1, 2, 4 absent from early diagnostic output) via boot log: genuine VM placement failures (`"No Suitable Host Found!"`) traced to `ConstructorVariableVM.MIPS_TIERS = {2000, 500, 1000}` containing a value (2000) exceeding what any single PE can supply (`PeProvisionerSimple(1000)`, `pesNumber=1` per VM) — structurally unsatisfiable for roughly 1/3 of the VM population (deterministic given the seeded RNG), independent of aggregate host/VM capacity math. **Fixed** by the user, tier value 2000 → 250.
9. Re-tested with the placement bug fixed: ratio still pinned at exactly 1.0 — isolates the demand-ratio degeneracy as a scenario-structural property, fully independent of the (separately real, separately fixed) placement bug.

**Outcome / pending work:**
- `demand` retirement decided; `vm.getCurrentRequestedTotalMips()` removed from approved API (Monitor, Planner).
- Planner spec's FURTHER CONTEXT needs to become **more directive** about the feasibility-check formula — the previous open-ended "must check whether destination has adequate resources" converged identically (and structurally incorrectly) across all 5 independent generations. Worth stating explicitly in any writeup as its own finding: vague feasibility instructions can converge on a shared blind spot rather than producing genuine diversity.
- Planner regeneration is necessary but **not sufficient alone** — demand-based Monitor/Analyser combinations remain `INERT`/dead-signal regardless of Planner quality, since the signal is uninformative before it reaches the Planner. Two fixes required in tandem: (a) Planner spec amendment + regeneration — universal, urgent, unblocks every non-demand combination; (b) demand metric retirement reflected in Monitor/Analyser specs too — narrower, resolves the remaining dead-signal rows.
- Full re-run against corrected specs/scenario **completed 2026-07-03** — see "5×5×5×3 Run — Cloudlet Migration, Corrected Specs (Current)" below.
- Executor spec's Approved API section still needs cleanup (stale entity-method listing left over from before the ID-only redesign).

---

### 5×5×5×3 Run — Cloudlet Migration, Corrected Specs (CURRENT, 2026-07-03)

Full 375-permutation sweep re-run against the fixed `moveCloudlet` signature and the `demand`-purged specs. All 375 rows completed (status ACTIVE throughout, no FAILED). The zero-execution problem is resolved: `actions_executed` now ranges 0–31; only 17/375 rows still land at 0 (all `monitor_v3 + {analyser_v2, analyser_v5}`); `conversion_rate` spans 0.0–1.0.

**Critical finding: the inherited ground-truth column (`average_cpu_demand_variance`) is completely non-discriminating.** All 375 rows report the identical value, 0.190647 (spread ≈ 2.8e-17 — floating-point noise only, not a real difference). This is structural, not a scenario quirk: the column is host-level variance of per-VM demand pressure, and cloudlet migration moves work *between VMs*, never *between hosts*. VM-to-host placement and each VM's rated MIPS are untouched by any cloudlet-migration action, so the host-level aggregate this metric measures literally cannot move regardless of what the pipeline does. Carrying the VM-migration family's ground truth over unmodified was a mistake — it was never capable of detecting this family's actions. Needs replacing with a cloudlet-migration-appropriate metric before it's used for anything, and before any cross-family/mixed-pool comparison (see "Design goal: a mixed pool" above) is meaningful.

**`makespan` is the metric that actually discriminates**, and conveniently it's also the metric this family's own rationale names as the target ("minimise makespan via cloudlet migration" — see Rationale above). 106 distinct values across 375 rows, range 1927.04–6149.35.

**6149.35 confirmed as the no-controller baseline for this specific single-scenario run (2026-07-03), but superseded as "the" family baseline — see below.** Originally inferred from dead-pipeline convergence (every `opportunity_cycles == 0` triple lands exactly on 6149.35), then independently confirmed against a direct no-controller run of the same scenario (per-cloudlet finish times topping out at 6149.35). Both are correct for the scenario this 375-row sweep used — the point stands as a methodology validation (dead-pipeline convergence is a trustworthy proxy for baseline). What's now known to be wrong is treating 6149.35 as a fixed constant across scenarios.

**Best (single-scenario, now superseded as a generalizable number): 1927.04ms** (`monitor_v4 + analyser_v3 + planner_v2`, 100% conversion) — a **68.7% reduction** vs. the 6149.35 ceiling for that one scenario.

**Scenario-seed parameterization bug, found and fixed (2026-07-03).** The 20-scenario re-run (built to test 10,000×N-scenario feasibility, see "10,000-Combination Scaling / Timing" below) showed `scenario_seed=42` producing dead-pipeline convergence at 6301.91, not the established 6149.35 — initially read as evidence that baseline genuinely varies even for a nominally-identical seed. Root cause was narrower than that: the scenario-parameterization rewrite fed the cloudlet-length generator `scenarioSeed + CLOUDLET_SEED_OFFSET` instead of `scenarioSeed` directly (an unnecessary attempt to "decorrelate" VM and cloudlet RNG draws — unnecessary because separate `Random` instances don't share state regardless of seed value, so there was never a correlation risk to guard against). This meant `scenario_seed=42` reused the original's VM layout but silently drew *different* cloudlets, making it a different scenario wearing the old label. Confirmed via the data before the fix was known: no `actions_executed > 0` row in the mislabeled "scenario 42" ever exceeded 6301.91 (max among active controllers there was 6102.14) — every row above 6149.35 was a dead pipeline correctly converging on *that* (different) scenario's true no-op value, not a harmful-controller effect. **Fixed**: `CLOUDLET_SEED_OFFSET` removed, `scenarioSeed` now passed unmodified to both `createVM` and `createCloudlet` — `scenario_seed=42` reproduces the original 6149.35 baseline exactly again.

**The underlying methodological point survives the fix, though, and still applies to every scenario seed *other* than 42.** There's no historical baseline to fall back on for scenario 43, 44, etc. — those baselines are genuinely unmeasured until you run them, and per the 20-scenario data (see "Cross-Scenario Stability" below), a ~10-15% CV between scenarios for real controllers is normal. So: compute baseline per `scenario_seed` (via dead-pipeline convergence, which the confirmed-correct 6149.35 case validates as a reliable proxy), and compute % improvement per-scenario before aggregating across the 10,000×N sweep — never against one fixed global constant, even though that constant is now correctly reproducible for seed 42 specifically.

| Slot | Best mean makespan | Worst mean makespan | Note |
|---|---|---|---|
| Monitor | monitor_v4 (2628) | monitor_v3 (3637) | monitor_v3 pairs dead with analyser_v5 |
| Analyser | analyser_v3 (2365) | analyser_v5 (3928) | reversal vs. VM-migration family, where analyser_v5 was dominant |
| Planner | planner_v4 (2575) | planner_v1 (3606) | planner_v1 now functional (was 0/375 before the fix) but still weakest active planner |

**planner_v1 fix confirmed effective.** Previously 0 actions executed across all 375 rows (unconditionally-unsatisfiable feasibility check, see Debugging Session above). Now executes 1–30 actions per pairing depending on monitor/analyser. It remains the weakest of the five active planners by mean makespan, which is now a genuine quality signal rather than a symptom of the earlier total-failure bug.

**analyser_v5 reverses roles from the VM-migration family.** There it was the dominant, distribution-agnostic performer (lowest mean variance of any analyser, never bottomed out at baseline). Here it's the worst (mean makespan 3928) and is entirely dead-signal (`opportunity_cycles = 0`) whenever paired with monitor_v3. Its mean-relative thresholding, which generalised so well across VM-level metrics in the other family, does not transfer cleanly to this family's signals — worth flagging as a limit on "GUID-agnostic/distribution-agnostic modules generalise" as a cross-family claim, not just a within-family one.

**GUID-compatible combinations rank better here, unlike the VM-migration family.** The 12 compatible rows (4 unique triples) average 2509 mean makespan vs. 3063 for incompatible — the opposite direction from VM migration, where compatibility didn't predict quality either way. Sample is small (4 triples), so treat as suggestive pending a larger compatible sample, not as a reversal of the "GUID compatibility does not predict performance" headline finding.

**Executor slot is almost, but not perfectly, non-discriminating.** 116 of 125 (monitor, analyser, planner) triples show zero makespan spread across the 3 executors, consistent with the VM-migration finding. 9 triples show nonzero spread (up to 311ms, `monitor_v3+analyser_v5+planner_v1`) — plausibly timing/tie-breaking sensitivity in edge-of-simulation migrations rather than genuine executor-logic differences, but not yet root-caused against source.

**Outstanding:** replace/supplement the ground-truth metric for this family (highest priority — the current column is dead weight); root-cause the 9-triple executor spread; Executor spec's Approved API cleanup (unaffected by this run, still pending from before). Baseline is now directly confirmed (see above) — no longer outstanding.

### 10,000-Combination Scaling — Timing, Design, and a Real Bug Caught by It (2026-07-03)

**Motivation.** Final framework plan: ~10 variants per module × 4 module types = 10,000 combinations. Question raised: run each against 10 or 100 random scenarios? Before answering, `ConstructorVariableVM.java` was rewritten to make scenario seeding an actual parameter (`NUM_SCENARIOS`, `SCENARIO_SEED_BASE`, scenario loop outermost so every module combination is evaluated against the identical set of scenario seeds — a common-random-numbers/blocked design) and to add per-run wall-clock timing (`System.nanoTime` around each `runSimulation` call, logged per row and summarised at the end), plus streaming CSV writes and progress reporting (the original file buffered all `SimulationResult`s and wrote them only after the full sweep — fine at 375 runs, a crash-safety and visibility problem at six-figure run counts).

**Timing result: both 10 and 100 scenarios are trivially feasible.** 375 runs completed in under 2 seconds by hand-timing; instrumented timing on a 20-scenario run (7,500 rows, 5×5×5×3 module space) showed heavy JIT-warmup skew in the first ~100 rows (row 0 alone: 116.9ms) but a flat steady state afterward: mean 0.61ms/run, median 0.46ms/run, no meaningful spread across planners or monitors. Extrapolated: 10,000×10 = 100,000 runs ≈ 1 minute; 10,000×100 = 1,000,000 runs ≈ 10 minutes. Recommendation: use 100 scenarios — no compute reason to compromise down to 10, and a larger scenario count directly reduces the ranking-instability risk documented below.

**Bug caught by the scenario-seed rewrite, not by inspection: `CLOUDLET_SEED_OFFSET`.** An early version of the rewrite fed the cloudlet-length generator `scenarioSeed + CLOUDLET_SEED_OFFSET` instead of `scenarioSeed` directly, intended to "decorrelate" VM and cloudlet RNG draws — unnecessary, since two separate `Random` instances never share state regardless of seed value. Net effect: `scenario_seed=42` in the new multi-scenario runs silently stopped reproducing the original hardcoded-`Random(42)` scenario (same VMs, different cloudlets), so its dead-pipeline convergence value drifted from the established 6149.35 to 6301.91. Confirmed as pure seed mislabelling, not a real baseline difference or a harmful-controller effect (no `actions_executed > 0` row in the mislabelled scenario ever exceeded 6301.91). **Fixed**: offset removed, `scenarioSeed` now passed unmodified to both `createVM` and `createCloudlet`; `scenario_seed=42` reproduces the original 6149.35 baseline exactly again.

**Methodological takeaway that survives the fix:** baseline should be computed **per scenario_seed** (via dead-pipeline convergence, now validated twice as a reliable proxy — once historically, once via direct no-controller confirmation), not treated as one fixed constant across a multi-scenario sweep. Seed 42 specifically is a known quantity again, but seeds 43, 44, ... have no historical reference and must be measured, not assumed.

### Cross-Scenario Stability Findings (2026-07-03, from the 20-scenario / 7,500-row re-run)

**Single-scenario rankings are demonstrably unreliable — quantified, not just theorised.** The triple that ranked best under scenario 42 alone (`monitor_v4+analyser_v4+planner_v4`, makespan 1728.55) is only the **23rd-best of 125** once averaged across all 20 scenarios (true best: `monitor_v4+analyser_v4+planner_v2`, mean 2300.09). This is the single-seed caveat already attached to `monitor_v1+planner_v5` in the VM-migration family, now directly demonstrated with real cross-scenario data rather than flagged as a theoretical risk.

**Consistent performers:** `planner_v3` and `planner_v4` are the most stable planners by coefficient of variation (mean CV 0.150 and 0.161 respectively, vs. 0.156–0.164 for planner_v1/v2 and 0.196 for planner_v5). Best combined mean-performance-and-stability triples: `monitor_v1+analyser_v5+planner_v4` (mean 2432.6, CV 0.107 — the single most consistent controller in the dataset) and `monitor_v4+analyser_v5+planner_v4` (mean 2398.3, CV 0.130).

**Highest volatility, and a concrete mechanism for it:** `planner_v5` (mean CV 0.196, worst-case 0.354) and `analyser_v2` (mean CV 0.200, same worst-case 0.354) are each independently the least stable module in their slot; combined (`monitor_v1+analyser_v2+planner_v5`) they produce the single most volatile controller in the dataset — mean makespan 4458, ranging from 2267 (near-best) to 8384 (worse than the no-controller baseline) across 20 scenarios. Mechanism, visible in the per-scenario breakdown: makespan tracks almost directly with how many migrations this pairing manages to execute, and that count itself swings from 1 to 14 depending purely on the scenario draw — not a clean dead/alive collapse (`opportunity_cycles` is nonzero in several bad-outcome scenarios too), but a pairing whose realised activity level is unusually scenario-sensitive. Adds a reliability dimension to `planner_v5`'s existing "weakest active planner, GUID-agnostic design" characterisation from the single-scenario run: it is not just weaker on average, it is also the least predictable, and `analyser_v2` specifically compounds this.

---

## Dissertation Context — Muiz Rusli (From Intent to Implementation)

### Key Difference from This Project

Muiz's system is a **single-function replacement policy**: given cache state, return the item to evict. One module slot, one API type, all modules trivially interchangeable. No pipeline, no closed feedback loop.

This project tests **LLM generation of control loop components that must interoperate across a sequential four-stage pipeline and produce emergent closed-loop behaviour**. The difficulty scales with inter-stage coupling.

### What This Project Contributes Beyond the Dissertation

- **Cross-GUID composition** — Muiz had only one API type; cross-family pairing was not a concept.
- **Multi-stage semantic coupling** — semantic mismatches producing silent failures rather than compile errors.
- **`LoadState` as underspecified contract** — structurally correct pipelines that fail silently due to semantic mismatch between metric-based classification and provisioning-based feasibility.
- **Metric invention** — LLM-generated modules that produce valid, meaningful metrics outside the specification boundary, distinct from API hallucination.
- **GUID self-labelling accuracy** — LLMs correctly self-label novel outputs rather than misrepresenting them as standard GUIDs.

---

## Power-Aware Family — Third Control Loop (Exploration, 2026-07-03)

### Rationale

Third of the three originally-named candidate systems goals (see Cloudlet Migration Family Rationale above): minimise power/energy use via VM migration. Chosen as the next family to explore after cloudlet migration reached a validated state. Distinct from the other two families in one important way: it reuses VM migration's existing action mechanism (`requestVmMigration` already exists, no new `ActionSpace` method needed, unlike cloudlet migration's `moveCloudlet`) — the novelty is entirely in the metering (power, not variance or makespan) and in the Planner's objective (drive hosts to complete emptiness, not balance or speed).

### Infrastructure: CloudSim's Power Package

`org.cloudbus.cloudsim.power` (+ `.power.models`, `.power.lists`) provides `PowerHost`/`PowerVm`/`PowerDatacenter` as drop-in extensions of the base `Host`/`Vm`/`Datacenter` classes, plus `PowerModel` implementations (`PowerModelLinear`/`Square`/`Cubic`/`Sqrt` — generic curve shapes — and `PowerModelSpecPower*` — real SPECpower-benchmarked hardware profiles). `PowerHost.getPower()` reads current draw; `PowerDatacenter.getPower()` (a plain public getter, not to be confused with the host-level method of the same name) accumulates **total energy for the whole run in Watt-seconds (Joules)**, computed automatically every processing cycle via `host.getEnergyLinearInterpolation(fromUtilization, toUtilization, timeDiff)` — no custom instrumentation needed for the top-line energy figure at all, unlike the demand-variance and makespan ground truths in the other two families, which are hand-computed.

**CloudSim 3.0 docs ≠ CloudSim 7.0.1 behaviour — confirmed by direct source inspection, not assumed.** This project runs CloudSim 7.x (confirmed: `HollowedControl` already imports `org.cloudbus.cloudsim.core.HostEntity`/`GuestEntity`, an entity-abstraction layer that does not exist in 3.0). Verified differences against the `7.0` tag on `Cloudslab/cloudsim`: `PowerHost` now `implements PowerHostEntity extends HostEntity` (interface layer new in 7.0); `getPower(double)` is `public` in 7.0, `protected` in 3.0; `getMaxPower()`/`getEnergyLinearInterpolation()` still exist and compute the same formula but are now **default methods on the `PowerHostEntity` interface**, not concrete methods on `PowerHost` itself; CloudSim 7.0 also added nested-container simulation support (`PowerGuestEntity`, `getMigrableContainers()`), irrelevant to this project but evidence of a substantial refactor lineage ("CloudSim 7G"), not a patch release. Lesson: verify against the actual pinned source before citing CloudSim API specifics — search results default to 3.0-era (2012) javadoc.

### Architecture Decision: No New Interfaces Needed

Initial instinct was to create parallel types (`PowerHollowedControl`, Power-specific `Monitor`/`ActionSpace` variants) to accommodate `PowerGuestEntity`/`PowerHostEntity`. **Confirmed unnecessary by reading the actual current `HollowedControl.java`**: every method in it (`requestVmMigration(GuestEntity, HostEntity)`, `getVmList(): List<GuestEntity>`, `getAllHosts(): List<HostEntity>`, `updateGroundTruth()`'s internals) is written exclusively against the base entity interfaces — never a concrete `Host`/`Vm`/`Datacenter` type. Since `PowerHost`/`PowerVm` already satisfy `HostEntity`/`GuestEntity` via the `PowerHostEntity`/`PowerGuestEntity` interface chain, they flow through the existing, unmodified `HollowedControl<M,D,A>` with zero changes required. Confirmed empirically, not just architecturally: `HollowedControl` running cloudlet-migration's own `monitor_v1`/`analyser_v1`/`planner_v1`/`executor_v1` against a `PowerHost`-backed scenario compiled and ran correctly with no modifications to the framework class.

**Root cause of the compile error that prompted the investigation** (`requestVmMigration(PowerGuestEntity, PowerHostEntity)` "must override or implement a supertype method"): Java requires *exact* parameter type matches for `@Override` — parameter types are invariant, unlike covariant return types. Narrowing `GuestEntity`/`HostEntity` to `PowerGuestEntity`/`PowerHostEntity` in a subclass creates an unrelated overload, not an override. Correct pattern (matching the project's own established `instantiate()`-style single-chokepoint philosophy): keep the override signature identical to the interface, downcast internally in the method body only where Power-specific behaviour is actually needed.

**General principle for the rest of this family's development:** the entity abstraction layer exists specifically so code written against `HostEntity`/`GuestEntity` keeps working when the concrete backing type changes. Default assumption should be "the existing interface already covers this," not "I need a parallel type" — every instance of the latter instinct so far (interfaces, `PowerHollowedControl`) turned out to be unnecessary.

### Ground Truth: Empty Host = 0W (Decided) and Two Library Bugs Found

**Decision: an empty (0-VM) host counts as powered off (0W), not idle-but-on.** Standard assumption in VM-consolidation literature (Beloglazov & Buyya and follow-on work) and makes the objective directly sensitive to consolidation. Turns out to be **built into `PowerModelLinear.getPower()` already** — it has an explicit `if (utilization == 0) return 0` branch, independent of `staticPowerPercent` — so no custom ground-truth code was needed to implement this decision at all.

**`HollowedControl.updateGroundTruth()`/`getGroundTruthAvgVariance()` is VM-migration-specific and hardcoded into the shared framework class — not applicable here.** Same root cause as why `average_cpu_demand_variance` went dead for cloudlet migration: it's one fixed calculation (host-level demand-pressure variance) baked directly into `HollowedControl`, not parameterised per family. For the power family this is simply ignored; the real ground truth is `PowerDatacenter.getPower()`, read externally after `CloudSim.stopSimulation()` — which accumulates correctly regardless of what `HollowedControl`'s internals do, since `PowerDatacenter` computes it independently every processing cycle.

**Bug 1 — `PowerDatacenter` scheduling interval of 0 causes an infinite zero-delay self-rescheduling loop.** `PowerDatacenter.updateCloudletProcessing()` re-enters its own "cancel and reschedule" branch whenever `cloudletSubmitted == CloudSim.clock()`; with `schedulingInterval=0`, each reschedule lands at the identical simulated timestamp, so the clock never advances past the moment cloudlets were submitted and the real per-cycle processing logic (energy accounting, `checkCloudletCompletion()`) is never reached. Symptom: simulation runs forever (alive, producing no further output) after the "Sending Cloudlet" log lines, must be killed manually. **Fixed**: use a small positive `schedulingInterval` (1) instead of 0.

**Bug 2 — CloudSim 7.0 library bug in `HostDynamicWorkload.getCompletedVms()`.** Confirmed by direct source inspection:
```java
public List<GuestEntity> getCompletedVms() {
    ...
    if (guest.getCurrentRequestedTotalMips() == 0) { vmsToRemove.add(guest); }
    if (guest instanceof VirtualEntity vm && vm.getNumberOfGuests()==0) { vmsToRemove.add(vm); }
    ...
}
```
The second condition — added for CloudSim 7.0's nested-container support — checks "does this VM have zero nested containers," which is unconditionally true for any ordinary VM (this project never uses nested containers), independent of whether its cloudlets have actually finished. Result: **every VM gets flagged for removal after the very first processing cycle**, regardless of real remaining cloudlet length — observed as all 12 VMs deallocating at t=1.01 while cloudlets (needing thousands of time units) had barely started. This is a genuine library regression, not fixable from calling code directly. **Fixed** via a narrow subclass (`FixedPowerHost extends PowerHost`) overriding only `getCompletedVms()` to drop the erroneous second condition, keeping the (correct) MIPS-demand check. Documented as a CloudSim-7.0-specific gotcha in Known Limitations below, in the same category as the earlier `getMips()` per-PE-vs-total trap.

Both bugs are now fixed; a no-migration baseline run completes correctly end-to-end (utilization stable and realistic across ~6115+ time units, matching the historical cloudlet-migration makespan for this scenario).

### Power Model Calibration

Initial parameterisation (`PowerModelLinear(10, 0)`) was a naive placeholder — `maxPower=10` has no real-world grounding, and `staticPowerPercent=0` removes the idle/static power floor entirely, which removes most of the reason consolidation saves energy in the classic literature (the whole argument rests on hosts carrying a large fixed cost just for being on — typically 60–70% of max power at idle — which is what makes packing VMs onto fewer hosts an actual win rather than a wash). **Recalibrated to `PowerModelLinear(250, 0.6)`**: 250W max power (plausible real-server range, matching the SPECpower-derived hardware profiles the CloudSim power package is built around) and 60% static-power fraction (150W idle floor), giving the objective a continuous gradient toward consolidation rather than an all-or-nothing reward only at the exact moment a host reaches zero occupancy.

### No-Controller Baseline (Validated)

Standard scenario (6 hosts, 12 VMs, 60 cloudlets, seed 42), no migrations permitted: **Total energy = 4,090,075 W·sec (≈1.14 kWh)**, makespan matching the historical cloudlet-migration baseline for this scenario (~6149, consistent with the non-power sweep's 6149.35). Cross-checked internally: instantaneous datacenter draw at one mid-run cycle (668.75W) × total simulated seconds (≈6116) ≈ 4.09M W·sec, matching the reported total almost exactly — confirms the accounting is self-consistent, not just plausible-looking.

### Key Finding: Two Distinct, Largely Orthogonal Energy-Saving Levers

Running cloudlet-migration's own `monitor_v1+analyser_v1+planner_v1+executor_v1` (zero power-specific logic, no VM migration at all — only cloudlet-to-cloudlet redistribution within the fixed initial VM/host placement) against the power-instrumented scenario produced **makespan=3100 (49.6% reduction) and total energy=2,006,243.3125 W·sec (50.9% reduction)** — nearly proportional to each other.

This reveals two separable levers, not one:
- **Temporal compression** — finishing the workload faster reduces the *duration* hosts must stay powered at a given level. Since host occupancy was completely unchanged in this run (no VM ever moved between hosts), the ~51% energy reduction came entirely from needing ~50% less total time, not from touching which hosts were on. This lever is *aligned* with makespan minimisation, achieved here "for free" by a controller with no energy awareness at all.
- **Spatial concentration** — reducing the *number* of occupied hosts at any given moment, eliminating static-power floors entirely for hosts driven to zero VMs. This is the classic VM-consolidation lever, and it is the one genuinely in tension with makespan (packing more VMs onto fewer hosts risks contention that lengthens completion time) — the tension flagged in the "would it be feasible to have a cost function" discussion applies specifically to this lever, not to temporal compression.

**Methodological consequence:** the no-controller baseline (4,090,075 W·sec) is the wrong bar for a power-aware Planner to clear. A plain makespan-optimising controller already gets ~51% of that "for free." The real baseline a power-aware Planner must beat is **2,006,243 W·sec** — it only earns its keep by additionally pulling the spatial-concentration lever on top of whatever temporal compression it also achieves.

### Planner Design Implications (Not Yet Implemented)

From working through "what actions would a controller take to minimise energy":
- **Prefer migrating a VM onto an already-occupied destination host over an empty one.** Moving onto a host that's already drawing its static floor costs nothing extra; moving onto an empty host wakes a fresh ~150W floor, which can erase whatever the source-host evacuation just saved.
- **Target complete evacuation of underloaded hosts, not partial de-loading.** With a static-power-dominant curve (60% of draw is fixed), reducing a host's utilization without emptying it captures very little of the available saving — the payoff is concentrated almost entirely at the zero-occupancy boundary.
- **Feasibility guard is mandatory, not optional**: `VmSchedulerTimeShared` has no hard rejection for oversubscription (unlike host-level VM placement), so "would this migration push the destination into OVERLOADED" must be checked explicitly before committing — same discipline already documented for `isSuitableForGuest` (guard, not selection criterion).
- **Statelessness (MAPE, not MAPE-K — an existing, previously-flagged framework limitation) bites harder here than in the other two families.** Fully evacuating a host with several VMs needs multiple consecutive migrations (one action per cycle, per the existing one-migration-per-cycle convention), and the framework has no memory between cycles to track "I'm partway through emptying host X." A stateless greedy Planner will likely still trend toward evacuation over time but may thrash between partially-evacuating multiple hosts rather than completing one.
- **VM scale-in is not currently a supported action.** The original candidate goal was "power via migration/**scaling**," but `ActionSpace` only has `requestVmMigration`/`requestVmCreation` — no VM termination. Migration-only is the current scope; scale-in would be a genuinely new action type, not an extension of an existing one.

### Roadmap (7 Phases, Tracked)

0. Sync authoritative source files — **done** (`HollowedControl.java` confirmed current and fully generic).
1. Confirm `PowerHost`/`HostEntity` integration + ground-truth semantics — **mostly done** (integration confirmed empirically; empty-host=0W confirmed built-in; no-controller baseline validated).
2. Define power-family GUID contract + module boundaries — **done, see "Hand-Coded Power-Minimizing Planner" below (2026-07-06)**. `LoadState` redefined per-Analyser as OFF/BALANCED/FULL rather than reusing the QoS UNDERLOADED/BALANCED/OVERLOADED semantics; metric is CPU demand (not VM count, not `host-power`), chosen because it's the same quantity `PowerModelLinear`'s zero-power branch keys on.
3. Hand-coded reference controller — **done (2026-07-06)**, see below. `Analyser9`/`Planner5`/`Executor5` implemented and validated against a corrected scenario; beats the relevant no-controller baseline by 13.6% (energy) once tested against a scenario that actually has consolidation headroom (see caveat below re: which baseline).
4. Generation specs + LLM variants — **done (2026-07-07)**. Monitor/Analyser/Planner specs (5 variants each, no new Executor generation), reviewed against epsilon thresholds / GUID naming / deliberate fallback-silence; approved API extended with the cloudlet exec-list chain. See "Power Family — LLM Generation Batch" below.
5. Small-scale validation sweep — **done (2026-07-07)**. Full 5×5×5 sweep run, three instrumentation defects found and fixed (each validated by exact reproduction of all 125 power values), all 15 module sources read and characterised. **Multi-scenario validation also done (2026-07-07)**: 10 scenarios, seed-42 findings confirmed — best generated controller 43.1% ± 4.1 mean energy reduction. See below.
6. Full sweep, merge with paused scenario-generalisation work (structural grid tasks) — not started. Multi-scenario prerequisite now met; remaining optional strengthening: 100-scenario run (~10 min), hand-coded reference through the same multi-scenario harness for a symmetric comparison.

### Hand-Coded Power-Minimizing Planner — Design, Implementation, and First Validated Result (2026-07-06)

**Design.** Reused the existing flat `Monitor → double[] → Analyser → LoadState[] → Planner → int[] → Executor` pipeline unchanged — no new interfaces, following the same "press the current data flow to the max" discipline already applied to the cloudlet-migration terminal-GUID rename. The only new element is a redefinition of `LoadState`'s meaning for this Analyser specifically: UNDERLOADED = OFF (host at/near zero CPU demand — genuinely idle, matching `PowerModelLinear`'s own `utilization==0 → 0W` branch), OVERLOADED = FULL (demand at/near the scheduler's hard ceiling of 1.0, per `VmSchedulerTimeShared`'s no-oversubscription guarantee), BALANCED = the only actionable bucket, containing undifferentiated migration sources and destinations.

**Metric choice: CPU demand, not VM count.** VM count was considered and rejected as the classification signal. `PowerModelLinear.getPower()`'s zero-power branch is keyed on aggregate CPU demand being zero, not on VM count being zero — a host holding one VM with no currently-executing cloudlet already reads 0 demand and already draws 0W under the real power model, identically to a literally-empty host. Since the classification should mirror what the reward function itself treats as "off," demand is the more faithful signal, not merely a simpler one. This also resolved an earlier concern about needing two separate metrics (count for OFF, demand for FULL, requiring an unbuilt Fusion stage) — demand alone, with two thresholds (≈0 and ≈1.0), covers both ends within the existing single-`double[]`-per-Monitor contract.

**Anti-oscillation mechanism.** The Planner ranks BALANCED hosts by (re-derived) demand and always migrates from the current minimum to the current maximum. Initially flagged as redundant re-derivation of what the Analyser already computed (the Planner only receives `LoadState[]`, not the underlying `double[]`), but retained deliberately: migrating a VM off the current minimum can only lower it further, and onto the current maximum can only raise it further, so the gap between that specific pair can only widen, never invert, in a single cycle — a constructive guarantee against oscillation, not just a convention. The duplicated demand formula (same arithmetic exists in both the Monitor and the Planner) is accepted as a known, low-risk coupling rather than resolved via an interface change.

**Modules implemented.**
- `Analyser9` — classifies `LoadState[]` from `double[]` demand using `FULL=1.0`/`OFF=0.0` exact-equality thresholds. **Known fragility, not yet fixed**: exact equality on `FULL` is risky given `VmSchedulerTimeShared`'s MIPS-share arithmetic could plausibly return e.g. `0.9999999999997` for a genuinely saturated host, silently misclassifying it as BALANCED. An epsilon-guarded comparison is recommended but not yet applied. GUID naming (`host-demand`/`host-demand-loadstate`) does not follow the established `host-<metric>-<type>` convention (`host-cpu-demand` would be consistent) — left unresolved, to be settled when the generation spec is written.
- `Planner5` — finds the min/max-demand BALANCED hosts (raw entity re-derivation, see above), migrates one VM from min to max, gated by `isSuitableForGuest`. Two bugs found and fixed during review: `mostDemand` sentinel initialized to `Double.MAX_VALUE` (never satisfies `demand > mostDemand`, planner permanently inert) → fixed to `0.0`; `if (demand < leastDemand) {...} else if (demand > mostDemand) {...}` prevented a single host from being evaluated against both running extremes, causing the true maximum to be lost whenever it happened to be the first host processed → fixed to two independent `if` statements. **Known, retained limitation**: `leastLoaded`/`mostLoaded` can resolve to the same host (exactly one BALANCED host, or an exact demand tie — plausible given the small discrete MIPS tier set) with no explicit guard against self-migration; and the Planner tries exactly one destination candidate per cycle with no fallback to a second-best host if the first fails the capacity guard. Both left unfixed deliberately (see below).
- `Executor5` — resolves the `int[]{vmId, hostId}` action (sentinel `{-1,-1}`) to entities and calls `requestVmMigration`, matching the established `executor_v1/v2/v3` pattern. (Root cause of an earlier `ClassCastException`: `Executor2`, from the older `List<VmMigrationPair>`-typed module generation, had been wired to `Planner5`'s `int[]` output — incompatible action types across module generations compile fine under the registry's raw-typed reflection pattern but fail at runtime.)

**Deliberate non-fix: destination fallback.** `Planner5`'s single-shot destination selection (no retry against a second-best candidate) is being left in the hand-coded reference as-is, and the corresponding generation-spec wording will stay silent on this behaviour rather than requiring a fallback — mirroring the cloudlet-migration family's "open-ended feasibility-check wording converged identically across all 5 independently-generated planners" finding. The intent is to see whether independent generations discover a retry/fallback approach unprompted, or replicate this exact limitation, as a genuine test of emergent behaviour rather than compliance with an explicit instruction.

**VM-allocation logging.** Built a one-time, `Log`-independent VM-allocation dump (`System.out.println`, bypassing the static `Log.disable()`/`enable()` state entirely) triggered on the first invocation of the MAPE observation loop inside `HollowedControl`. Necessary because (a) CloudSim's own internal allocation logging was getting buried under subsequent per-tick log volume before terminal scrollback could be reviewed, and (b) the driver's `main()` cannot observe intermediate simulation state at all — `CloudSim.startSimulation()` is a single blocking call, so `main()` only ever sees "nothing placed yet" or "the entire run, including every migration, already finished." The hook has to live inside a `SimEntity` participating in the event loop.

**First test run — First-Fit placement (`VmAllocationPolicySimpler`), same scenario as the QoS family.** Total energy 4,079,375.0 W·sec / avg power 657.96 W, against a no-controller baseline of 4,090,075 W·sec / ~665 W — a ~0.26% reduction, indistinguishable from noise. Diagnosed via the allocation log and per-cycle Analyser output: only two hosts were ever BALANCED (Host0 at 0.9375, Host2 at 0.25, frozen across every logged cycle), and the resulting Host2→Host0 migration attempt was rejected every single cycle by `isSuitableForGuest` — the same "checks provisioned capacity, not runtime utilisation" gap already documented for the QoS family, recurring here. With no fallback destination, the planner produced nothing for the whole run.

**Root-caused why First-Fit is a poor scenario for this objective.** Direct arithmetic on the logged allocation (Host0 = 3750/4000 MIPS committed, Host1 = 4000/4000 exactly, Host2 = 1000/4000, Hosts 3–5 empty) showed the First-Fit placement was already at or very near a local optimum for this specific 12-VM population: Host1 has zero headroom, and Host0's only free headroom (250 MIPS) is smaller than the one VM (1000 MIPS) sitting alone on Host2 — no feasible migration could improve on it. This also explains why avg_power figures near 658–668 W have shown up repeatedly across unrelated-looking experiments (the cloudlet-migration sweep's near-flat ~668 W band, and this run's 657.96 W): all of them are measuring the same static First-Fit placement, since neither cloudlet migration nor this first power-planner attempt ever changes which host a VM lives on. **Methodological note**: First-Fit was deliberately chosen to create imbalance for the load-balancing/QoS objective, but "fill early hosts before touching later ones" is itself a crude consolidation heuristic — the same scenario that stress-tests a balancing controller is close to a best-case, not worst-case, starting condition for a power/consolidation controller. The two families need materially different placement policies to be meaningfully tested; First-Fit only serves one of them.

**`VmAllocationPolicySimple` / `SelectionPolicyLeastFull` — a real CloudSim library bug for initial placement.** Attempted swapping to the stock `VmAllocationPolicySimple` (constructed with `SelectionPolicyLeastFull`) expecting a spread-out, Worst-Fit-style placement. First attempt produced an allocation byte-identical to First-Fit. Root-caused via source inspection: `SelectionPolicyLeastFull` computes host "availability" for `PowerHost` instances as `1.0 - powerHost.getUtilizationOfCpu()`. At the moment CloudSim resolves initial `VM_CREATE` events, no cloudlet has executed anywhere yet, so every host reads exactly 0 utilization — a universal tie. Combined with a strict `>` comparison (never `>=`) and stable list-order iteration, the tie never breaks, so the policy always returns the first not-yet-excluded host — indistinguishable from First-Fit, purely as a tie-break artifact, not a real Worst-Fit decision. This is a genuine limitation of the stock CloudSim 7.x class specific to *initial* placement (its utilization-based signal is only meaningful once workload is already running, e.g. for live-migration destination selection). **Fix**: a custom selection policy (`SelectionPolicyLeastFullByCapacity`) using the scheduler's static/provisioned `getAvailableMips()` — already present as the class's own non-`PowerHost` branch — instead of the `PowerHost`-specific utilization branch. Same sentinel-initialization bug pattern as `Planner5` (`maxAvailable` starting at `0` instead of `-1`) fixed in the same pass.

**Second test run — Least-Full/capacity-based placement, same controller.** No-controller baseline: makespan 6117.01, total energy 6,842,275.0 W·sec, avg power 1118.57 W (sanity-checked by hand: ≈36.5% average utilization across all 6 now-active hosts × [150 + 100×0.365] ≈ 1119 W — matches almost exactly). With the controller: makespan 6200.01, total energy 5,909,384.6875 W·sec, avg power 953.13 W — a **13.6% total-energy reduction, 14.8% average-power reduction**, at the cost of a **1.4% makespan increase**. This is the first real, credited result for this planner (the First-Fit run's 0.26% was noise). The makespan cost is read as the first concrete appearance of the already-documented "spatial concentration risks contention/makespan" tension, not a red flag on its own.

**Migration trace reconstructed exactly** by diffing VM-allocation snapshots at t=100 and t=6100: exactly two migrations occurred over ~61 possible cycles — VM3 (Host3→Host0) and VM1 (Host1→Host0) — correctly driving Host3 to true zero-VM/OFF. The run then stalled: the next min/max pairing (Host1's remaining VM, 1000 MIPS, vs. Host0, now with only 750 MIPS free) fails `isSuitableForGuest` every subsequent cycle, and with no fallback destination, nothing further happens for the rest of the run — even though Host2 (2750 MIPS free at that point) had ample room and a second host could plausibly have been fully drained too. This is empirical confirmation, not just a theoretical worry, that the single-shot destination limitation (see "Deliberate non-fix" above) is the actual binding constraint on how much of the available consolidation gets captured in this scenario.

**Caveat**: single scenario/seed. Per the existing cross-scenario stability findings for the other two families, the 13.6%/14.8% figures should be treated as promising, not validated, until re-run against multiple scenario seeds.

**Next steps agreed (2026-07-06)**: generate a 5×5×5×3 batch for this family (Monitor/Analyser/Planner variants only — no new Executor generation, reusing the existing VM-migration executors given their already-established non-discriminating status) against the Least-Full + capacity-fix scenario, paired deliberately the way First-Fit was paired with the QoS family. No new System Context needed. User is drafting the Monitor/Analyser/Planner Specification documents (adapting the existing QoS specs) for review against: epsilon-guarded (not exact-equality) thresholds, GUID naming consistency (`host-cpu-demand` convention still unresolved as of this writing), and the Planner spec deliberately staying silent on destination-fallback behaviour (see "Deliberate non-fix" above).

### Forward-Looking: Multi-Objective Fusion (Not Scheduled — Phase 7 Candidate)

Raised as a design question: could a Monitor read multiple signals (cloudlet ETC, host demand, host power) feeding an Analyser-level cost function, producing a controller that optimises a genuine trade-off across makespan/energy/demand rather than a single objective? Assessed as architecturally feasible with a specific recommended shape: a new **Fusion** stage between the Monitor(s) and the existing Analyser, combining several `double[]` Monitor outputs into one composite `double[]` score — leaving `Analyser`/`Planner`/`Executor` contracts completely unchanged, since LoadState classification works identically on a composite score. Two concrete prerequisites, not yet solved: (a) **indexing-domain alignment** — cloudlet ETC is VM-indexed, host demand/power are host-indexed, so Fusion needs an explicit reduction step (e.g. aggregate each host's VMs' ETC) before any elementwise combination is meaningful; (b) the GUID convention's one-to-one string-equality compatibility check would need extending to multi-input matching (Fusion's declared input GUIDs as a set, not a single string) — a second, larger extension of the same kind already done once for cloudlet migration's terminal-GUID reinterpretation. Framing note: with makespan and energy shown to be in genuine tension via the spatial-concentration lever (see above), a composite cost function is properly understood as selecting a point on a Pareto frontier via chosen weights, not converging on one universally "most optimal" state — and characterising which point an LLM-generated composite Analyser implicitly lands on (declared weights vs. actual behaviour) is a natural extension of this project's existing "declared GUID vs. actual internal logic" line of findings (e.g. `planner_v5`). Not started; contingent on all three families being independently validated first. **Update (2026-07-06)**: with the power-family hand-coded planner now validated (13.6% energy reduction, see above) and demand-vs-count and the QoS/power tension both concretely demonstrated, the "all three families independently validated" gate is close to met (hand-coded validation exists for all three; the power family's own 5×5×5×3 generation batch is the remaining piece, now in progress). Sequencing confirmed: pursue Fusion after that batch is independently characterised, not before.

### RAM/BW as Additional Metrics — Assessed, Not Yet Implemented (2026-07-06)

Raised as a design question: could RAM or BW be worked into new metrics, and what would optimising them achieve? Two findings, both worth acting on before any implementation:

**Currently degenerate, not just unused.** `createVM` in the current harness hardcodes identical `ram=512`/`bw=1000` for every VM regardless of MIPS tier. Since every VM has an identical footprint, a host's RAM or BW utilization ratio is currently *exactly* proportional to VM count — it would carry zero information beyond what VM count already tells you. This is the same category of trap as the cloudlet-migration family's degenerate `demand` ratio (a signal that looks real but is actually a constant proxy for something else, due to a specific structural property of the scenario). To make RAM/BW genuinely informative, VMs need heterogeneous footprints — a `RAM_TIERS`/`BW_TIERS` array mirroring `MIPS_TIERS`.

**Three distinct candidate goals, not one.** (a) *Multi-resource load balancing* — extending the QoS objective across CPU/RAM/BW together; largely already built (`Analyser5`'s weighted `cpu_util`/`ram_util`/`bw_util` composite already exists) but never the focus of a dedicated generation batch. (b) *Stranded-capacity/fragmentation minimisation* — genuinely new: track the ratio between a host's resource dimensions (is its RAM:CPU balance close to the population average or skewed?) and migrate to correct dimensional skew rather than overall load level — the classic multi-dimensional bin-packing problem, not yet explored here at all. (c) *Feasibility diagnosis* — expose RAM/BW as explicit Monitor signals so `isSuitableForGuest` rejections can be attributed to a specific resource dimension instead of being an opaque black box; a direct extension of the existing "isSuitableForGuest checks provisioned capacity, not runtime utilisation" line of findings.

**No causal connection to the power objective.** `PowerModelLinear` computes power from CPU utilization alone — no RAM term. A RAM-driven controller would not be an alternative lever for the *existing* power objective; it would only move power incidentally via correlation (if RAM-heavy VMs tend to also be CPU-heavy). Genuinely coupling RAM to power would require extending the power model itself (some real hardware power models do include a smaller memory term) — otherwise this is a third, separate objective, not a variant of QoS or power.

Not started; flagged as straightforward to add later given the existing `Analyser5` groundwork, contingent on giving VMs heterogeneous RAM/BW footprints first.

### UtilizationModel Investigation — Resolved: Genuinely Live, But Structurally Unread by CPU Demand (2026-07-07)

Triggered by an unexpected result: swapping `UtilizationModelFull` → `UtilizationModelStochastic` in `createCloudlet` produced zero observable change in demand, makespan, power, or energy. An initial hypothesis that `UtilizationModel` was "vestigial" in this framework was explicitly challenged (correctly) and revised through direct source investigation rather than accepted at face value.

**Two parallel, independent computation pathways confirmed via source** (`CloudletSchedulerTimeShared.java`, `PowerHost.java`, `PowerVm.java`, `HostDynamicWorkload.java`):
- **Allocation-based pathway** — feeds `PowerHost.getPower()`, demand, and makespan. Routes through `CloudletSchedulerTimeShared`'s fair-share MIPS allocation, which never references `UtilizationModel` at all (`getTotalCurrentRequestedMipsForCloudlet`/`getTotalCurrentAllocatedMipsForCloudlet` = `getCurrentCapacity() * cl.getNumberOfPes()`).
- **Cloudlet-utilization-model-based pathway** — `PowerVm.updateCloudletsProcessing()` → `getTotalUtilizationOfCpu()` → per-cloudlet `cl.getUtilizationOfCpu(time)`, feeding `utilizationHistory`. Genuinely reads whichever `UtilizationModel` is plugged in — but this history is only consumed by CloudSim's own built-in VM-consolidation heuristics, which are disabled in this project via `datacenter0.setDisableMigrations(true)`.

**Confirmed empirically, not just architecturally**, via a one-off diagnostic dump of `vm.getUtilizationHistory()`: the Full model produced clean, monotonically-decreasing integer sequences (literal concurrent-cloudlet counts, consistent with the earlier "`UtilizationModelFull` = cloudlet headcount" finding — see CloudSim Observability Constraint below); Stochastic produced continuously-varying, non-integer sequences. The write-side genuinely differs between the two models; nothing in the active module stack (`Monitor7`/`Analyser9`/`Planner5`/`Executor5`) reads it.

**Follow-up: built a new cloudlet-level Monitor metric** to make `UtilizationModel` a live lever rather than leaving it permanently unreadable. Per VM: average (not sum — summing reproduces the same unbounded headcount artefact as the built-in pathway) `cl.getUtilizationOfCpu(now)` over `vm.getCloudletScheduler().getCloudletExecList()`, scaled by `vm.getMips()`; summed and normalised per host by `host.getTotalMips()`. One bug found and fixed: an empty exec list (idle VM) divides by zero, and the resulting `NaN` silently poisons the entire host's sum via `NaN`-propagation through addition — guarded with `execCount > 0 ? ... : 0.0`.

Validating this new metric surfaced a much larger, independent finding — see below.

### CPU Demand Metric Is a Placement Signal, Not an Activity Signal — Root Cause Found (2026-07-07)

While validating the cloudlet-level metric above, a late-simulation window (t=5000–5900, same scenario as the hand-coded power planner) showed the new metric correctly reading exact `0.0` on every host whose VMs had zero cloudlets in their exec list — confirmed against the actual cloudlet finish-time table (all cloudlets on the affected hosts finish by t≈3800 at the latest) — while `vm.getCurrentRequestedTotalMips()`-based demand stayed frozen at a nonzero constant for the entire run, on every host, for thousands of time units after all work provably finished.

The frozen value was shown to be an exact identity, not an approximation: `requestedMips/totalMips` matched `Σ vm.getMips() / host.getTotalMips()` to full precision on every host, every tick. First hypothesis — `Vm.isBeingInstantiated()` stuck `true`, since `Vm.getCurrentRequestedTotalMips()`'s instantiation branch returns exactly `getMips()*getNumberOfPes()` — was tested directly (`vm.isBeingInstantiated()` printed every cycle) and **disproven**: reads `false` from t=100 onward, well before the freeze even sets in. Revised immediately rather than defended.

Root cause found in `CloudletScheduler.java` (abstract base class):
```java
public double getCurrentRequestedTotalMips() {
    double mips = 0.0;
    if (currentMipsShare != null)
        for (double v : currentMipsShare)
            mips += v;
    return mips;
}
```
`currentMipsShare` is set once per tick from the `mipsShare` parameter passed into `updateCloudletsProcessing(currentTime, mipsShare)` — the host-level `VmScheduler`'s allocation decision for that VM, not anything computed from the cloudlet exec list or `UtilizationModel`. Under `VmSchedulerTimeShared`'s no-oversubscription policy, a VM is granted its full requested MIPS ceiling the moment it's placed on a host with room, and that allocation does not shrink when the VM's workload finishes — it is a placement-time fact, not a live-activity signal, despite the misleading method name.

**Consequences:**
- `UNDERLOADED`/OFF (demand == 0) in the power family's `Analyser9` is only reachable when a host has zero VMs assigned — never when a host holds VMs that have simply finished their work. Host-emptying (as achieved by the hand-coded planner's 13.6% result, above) is detected correctly; idle-but-present VMs are invisible to the current metric by construction, not by omission.
- The "frozen demand, no further migration" pattern observed earlier in this project (both cloudlet-migration and power families) is now understood as a structural property of this metric, not scenario-specific equilibrium.
- The existing "monitor_v1/monitor_v2" note below, describing `getCurrentRequestedTotalMips()` as "deterministic and stable: reflects VM workload," is corrected by this finding — it is stable because it reflects VM *placement*, not because it robustly tracks workload.
- **Asymmetric finding, useful for future metric design**: `CloudletScheduler.getCurrentRequestedUtilizationOfRam()`/`getCurrentRequestedUtilizationOfBw()`, immediately adjacent in the same source file, iterate `cloudletExecList` directly and call `cl.getUtilizationOfRam/Bw(time)` — no allocation-share indirection. These correctly read 0 for idle VMs and genuinely reflect whichever `UtilizationModel` is active. The earlier "RAM/BW is currently degenerate" finding (hardcoded identical VM footprints, above) is a separate, orthogonal issue — even setting that aside, the RAM/BW accessor mechanism itself does not share the CPU-demand accessor's placement-vs-activity conflation.

**Not yet decided**: whether to adopt the new cloudlet-level demand metric (or an equivalent RAM/BW-based one) as a genuine replacement/companion signal in a future Monitor variant — doing so would require expanding the approved API to include cloudlet-exec-list access.

### Power Family — LLM Generation Batch, 5×5×5 Run (2026-07-07)

#### Pre-generation decisions (from the UtilizationModel / placement-signal findings above)

- **Stochastic utilization models deferred as future work.** Confirmed that in the current framework `UtilizationModelStochastic` adds no simulation complexity at all: the allocation-based pathway driving demand, makespan, power, and energy never reads the `UtilizationModel`, and nothing in the module stack read the write-side (`utilizationHistory`). Swapping Full→Stochastic is a no-op until activity-based signals are read — which this batch now does, making the model a live lever for a future batch.
- **Approved API extended with the cloudlet exec-list chain**: `vm.getCloudletScheduler().getCloudletExecList()` (non-destructive peek only) + `cl.getUtilizationOfCpu(double time)`, for Monitor and Planner. This exposes activity signals (exec-list membership/count genuinely vary under `UtilizationModelFull` even though per-cloudlet utilization is constant 1.0). Precedent: the cloudlet-migration family already had this chain approved. `host.isSuitableForGuest` removed from the Monitor list (Planner-only, as in the QoS family).
- **RAM/BW accessors deliberately excluded**: under uniform VM footprints + `UtilizationModelFull`, `getCurrentRequestedUtilizationOfRam/Bw` returns the concurrent cloudlet headcount wearing a RAM label — no new signal, guaranteed mislabelling bait. Revisit only after heterogeneous `RAM_TIERS`/`BW_TIERS` exist.
- **Metric vocabulary settled (resolves the naming question left open 2026-07-06)**, restoring the `host-<metric>-<type>` convention: `host-cpu-demand` (placement demand — same GUID as QoS monitor_v2, deliberately, for mixed-pool comparability), `host-cpu-activity` (execution-gated demand — new type token completing the util/demand/activity triple), `host-vm-count` (unchanged from QoS). The spec mandates the empty-exec-list NaN guard explicitly (no experimental value in leaving it open — a NaN'd monitor is garbage, not variation); stays silent on destination fallback and on `isSuitableForGuest`'s role (deliberate, see hand-coded planner's non-fix).

#### Results (Least-Full/capacity-fix scenario, seed 42)

375-row first run; re-run as 5×5×5×1 after confirming the executor slot perfectly non-discriminating (0 of 125 triples with any spread — third family confirmation). All rows completed, no FAILED.

- **Baseline: 1118.57 W avg power**, matching the validated no-controller baseline exactly. Dead-pipeline convergence proxy validated a third time: every INERT pipeline lands on it precisely.
- **Best: 705.35 W = −36.9%** vs baseline — **beats the hand-coded reference (953.13 W, −14.8%) by 2.5×** on captured savings. 11 triples hit it; planner_v3 dominates (best with monitor_v1/v2 across analysers v1–v4; also monitor_v5+analyser_v5+planner_v2/v5).
- Modal outcome 828.37 W (−26%, 43 triples). **No controller performs worse than baseline.**
- **Makespan constant (6117.01) across all 375 rows** including 9-migration runs — no contention cost appears anywhere, unlike the hand-coded run's +1.4%. Consequence: avg_power and total energy are proportional in this dataset; conclusions identical for both.
- Only **10 distinct avg_power values across 125 triples** — many pipelines converge on identical migration sequences; outcome space is coarse.
- **`average_cpu_demand_variance` anti-correlates with avg_power at r = −0.94**: best power controllers produce the *highest* placement variance (0.143 vs 0.007 baseline). The QoS and power objectives are near-perfectly opposed on this scenario — the concrete Pareto-tension number for the Fusion discussion.
- Opportunity cycles uncorrelated with outcome (r ≈ −0.02); best triples execute 6–8 migrations over 61 cycles. "Fewer, well-chosen migrations" replicates for a third family.

#### Three instrumentation defects found and fixed (each validated by exact reproduction of all 125 power values on re-run)

1. **`compatible` = false on all 375 rows.** The reused VM-migration executors still declared `inputGuid: "host-migration-pair"` (pre-rename) vs the planners' `vm-migration` — one stale terminal link zeroed the AND for every row. Fixed. Recommendation adopted going forward: log three per-boundary booleans instead of a single AND so a broken link can't silently kill the axis again.
2. **`actions_proposed` constant at 61 on every row.** The proposal predicate tested against the *cloudlet-migration* family's 3-element sentinel (`new int[]{-1,-1,-1}`); this family's 2-element arrays never match, so the predicate fired every cycle. Fixed to the 2-element sentinel (length-agnostic "any element ≠ −1" form recommended for the mixed pool).
3. **Actionable/opportunity gates redesigned for power semantics.** The inherited OVER∧UNDER opportunity gate read 0 on 48 rows that executed migrations — it encodes QoS source/destination logic, while this family acts on BALANCED hosts. New gates: **actionable = ≥1 BALANCED** ("not at goal state" — all-OFF-or-FULL is simultaneously the consolidation fixed point and a no-action state, since FULL can't receive and OFF can't donate), **opportunity = ≥2 BALANCED** ("a source–destination pair exists"). After the fix, the 15 INERT triples coincide *exactly* with the baseline-convergent set (monitor_v5 × analyser_v1/v2/v4) — the gate correctly identifies dead pipelines for the first time, zero false positives either direction. Side effects: status column not comparable to this family's earlier CSVs; a free derived metric ("first cycle at which actionable goes to 0" = time-to-goal) is available but analyser-relative — compute from ground truth if it's ever used as a claim.

This is the third instance of the "instrumentation carried across families doesn't transfer" pattern (dead variance column in the cloudlet family, quantized makespan under PowerDatacenter, now sentinel shape + gate semantics). Counter predicates, sentinels, and gates must be re-derived per family, not inherited.

**New counter finding once fixed: `actions_proposed == actions_executed` on all 110 active rows** — zero execution failures anywhere. Every generated planner proposes only migrations that pass `isSuitableForGuest` and resolve cleanly (contrast: the hand-coded planner spends most of its run re-proposing one infeasible migration). Planner selectivity, not execution success, is the discriminating quantity in this family.

#### Source-read findings — Monitors

Pool: v1 placement demand (uncapped, `host-cpu-demand`), v2 placement demand (clamped to [0,1], same GUID), v3 activity demand (sum — the spec formula, NaN guard correctly implemented, `host-cpu-activity`), v4 activity demand (**peak**: busiest VM only, same `host-cpu-activity` GUID), v5 VM count (`host-vm-count`).

- **monitor_v1 ≡ monitor_v2, byte-identical across all 125 pairings — "illusory variant diversity", new taxonomy entry.** The only difference is v2's clamp to 1.0, which is dead code: `VmSchedulerTimeShared`'s no-oversubscription invariant already caps the ratio at 1.0 by construction. v1's docstring explicitly claims the uncapped ratio "can exceed 1.0 when a host is overcommitted" — a legal-code, sound-reasoning, **false-in-this-environment premise** (true under `VmSchedulerTimeSharedOverSubscription`, which the spec never ruled out). Distinct from API hallucination and from metric invention: declared design differences along a dimension the environment makes unreachable, detectable only by knowing the scenario's structural invariants. Also means the batch effectively spent two of five slots on one metric.
- **monitor_v4 reuses v3's GUID for a different statistic** (max-per-VM vs sum, different scale: ≤0.25 vs ≤1.0 here). Not dishonest — the convention doesn't specify aggregation — so this is a **GUID vocabulary underspecification finding**, sibling to the LoadState one: `host-<metric>-<type>` distinguishes what is measured but not how it is aggregated; GUID-matched analysers can be calibrated to the wrong scale by a label-identical monitor.
- **Placement monitors (747 W mean) beat activity monitors (~815 W) — metric–reward alignment, not signal quality.** `PowerModelLinear` reads the allocation/placement pathway, so idle-but-present VMs still draw power; activity analysers classify such hosts ≈0 → OFF → excluded as migration sources → the cheapest, still-billing evacuation candidates become invisible. The sweep prices the OFF-definition conflation flagged at spec review at roughly 70 W. Corollary: under an activity-based power model (as real hardware behaves), the ranking would plausibly invert — this is a finding about reward-function structure.
- **monitor_v5 (count) is bimodal, fully explained**: raw counts fed to fixed [0,1]-scale thresholds classify every host FULL/OFF, no BALANCED → INERT (the 15 INERT triples exactly). Adaptive or unreachable-threshold analysers (v3/v5) turn the same signal into best-tier results. Replicates the QoS scale-mismatch/adaptive-analyser finding.

#### Source-read findings — Analysers

All five are the **same single-pass template** — `≤0.0 → UNDERLOADED; ≥ threshold → OVERLOADED; else BALANCED` — differing only in threshold (0.85, 0.80, 8.0, 0.60) plus v5's adaptive mean×1.2-of-active-hosts. Structural convergence (contrast the QoS batch's fixed/stddev/IQR/mean-relative diversity), benign this time. Declared GUIDs: v1/v4 `host-cpu-demand`, v3 `host-vm-count`, v2/v5 activity (see drift below).

- **The epsilon fragility did not materialise**: no module tests `== 1.0`; every FULL test is `≥ 0.60–0.85`. The generated modules are more robust than the hand-coded Analyser9 on exactly the point flagged as its known fragility.
- **analyser_v3's OVERLOADED is nearly unreachable** (`count ≥ 8` with 12 VMs / 6 hosts) → occupied hosts are always BALANCED regardless of input scale — *accidentally* scale-agnostic through threshold unreachability, a different mechanism from v5's genuine adaptivity, but the reason it "rescues" monitor_v5.
- **Cross-batch GUID vocabulary drift — root-caused as spec error, then fixed.** The monitor batch labelled the activity metric `host-cpu-activity`; the analyser batch (v2/v5, whose docstrings unambiguously describe the activity ratio) declared `host-vm-demand`, taken from the spec's older "VM demand" metric name — the vocabulary rename was not carried over to the analyser spec before generation (user-confirmed spec inconsistency, not LLM label instability). Fixed by **manual post-generation edit of analyser_v2/v5's GUID strings** — recorded here for provenance, since published compatibility numbers depend on hand-corrected labels. After the fix: **14/125 triples compatible (11.2%)**. Methodological lesson either way: GUID self-assembly across separately generated module batches stays consistent only if the metric vocabulary is pinned to exact tokens in *every* spec document; the QoS family never hit this because its vocabulary was fully enumerated.

#### Source-read findings — Planners

Declared input GUIDs: v1/v4 `host-cpu-demand-loadstate`, v2/v5 `host-vm-count-loadstate`, v3 `host-cpu-activity-loadstate` (all predicted correctly from compatibility data before reading source). All output `vm-migration`.

- **All five converged, unprompted, on the hand-coded architecture**: BALANCED-only source and destination, min-metric source selection, migrate toward max/tightest/first-fit destination, sentinel otherwise. Spec was silent on strategy; the design space collapsed to one shape parameterised by (source metric × VM-selection rule × destination heuristic).
- **The deliberate destination-fallback non-fix experiment has its answer: all five discovered candidate iteration unprompted.** Every planner scans all destinations with `isSuitableForGuest` as a filter *inside* the loop (first-fit: v1/v5; best-of: v2/v3/v4). None replicated the hand-coded single-shot stall — the opposite outcome from the cloudlet family's convergent feasibility blind spot. This is why proposed == executed everywhere and the main mechanism behind beating the reference 2.5×: the reference stalls when its single candidate is rejected; these keep looking.
- **`isSuitableForGuest` used correctly as a final guard by all five, unprompted** — the QoS family's documented misuse pattern (guard-as-selection-criterion) did not recur despite the spec deliberately staying silent on its role.
- **All five planners' internal logic matches their declared inputGuid** (v1/v4 score by `getCurrentRequestedTotalMips`, v2/v5 by guest-list size, v3 by exec-list utilization) — the spec's "further analysis must match the input analysis" clause held across the whole batch, unlike QoS planner_v5.
- **Why planner_v3 wins — the batch's central finding.** It is the only planner whose internal scoring re-derives *activity* (exec-list utilization via `ReadSpace`): least-active BALANCED source, evacuate that host's highest-contributing VM, most-active suitable destination. Its winning pairings put a *placement* monitor upstream — and the hybrid is why: placement-based classification keeps idle-but-present hosts in the BALANCED pool (activity analysers would misclassify them OFF), then activity-based scoring targets exactly those hosts as least-active sources. **Activity is a bad classification signal but the best planning signal, and the top controller uses each where it works.** Second-family replication of the "best module re-derives its own signal from raw entities" pattern (QoS planner_v5) — but here the declared GUID honestly matches the internal logic: contract-compliant GUID transcendence rather than mislabelling.
- **GUID compatibility still does not predict performance (third family).** Best triple (monitor_v1+analyser_v1+planner_v3, 705.35) is incompatible; compatible mean 776.05 W vs incompatible 822.08 W — compatibility filters some junk, doesn't find winners. planner_v3 appears in zero compatible triples even after the vocabulary fix (no analyser outputs `host-cpu-activity-loadstate` with a matching upstream monitor in the winning pairings).

#### Multi-Scenario Validation — 10 Scenarios, Findings Confirmed (2026-07-07)

125 triples × 10 scenarios (seeds 42–51) in the multi-scenario harness, 1,250 runs. This was the single-seed caveat's required check, and unlike the cloudlet family's equivalent (where the seed-42 best fell to 23rd of 125), **the power family's seed-42 findings survive essentially intact**:

- **Per-scenario baselines via dead-pipeline convergence: validated at scale.** The 15 INERT triples converge to exactly one avg_power value in *every* scenario (nunique = 1, all 10 seeds; baselines 1031.11–1118.57 W, ~8% spread). Per-scenario baselines therefore come free with every sweep; % reductions below are computed per-scenario before aggregating, per the established discipline.
- **planner_v3's dominance confirmed and strengthened.** The top 7 triples by mean per-scenario reduction are all planner_v3, at **43.1% mean reduction (±4.1 s.d., CV 7.8%)** — seed 42 was a slightly below-average scenario for them. Per-slot means: planner_v3 37.1% > planner_v5 36.1% > planner_v2 34.8% > planner_v1 33.0% > planner_v4 29.6%.
- **Ranking stability is much better than the cloudlet family's.** The 11 seed-42 best triples rank [1–7, 18, 22, 27, 28] of 110 active cross-scenario. And the best planner is *not* the most volatile this time: planner_v3 combines top mean with mid-pack CV (8.9%); planner_v4 is most stable (5.6%) but weakest; planner_v5 least stable (10.3%).
- **Placement > activity monitors replicates 10/10 scenarios** (typically 3–8 points of reduction) — the metric–reward alignment finding is not a seed artifact. Count (monitor_v5, 36.4% among active pairings) sits level with placement (36.3%): occupancy-based signals fully suffice when the reward is placement-keyed. monitor_v1 ≡ monitor_v2 to the decimal in all 10 scenarios (illusory-diversity collapse persists, as it must).
- **GUID compatibility fails to predict quality cross-scenario as well**: compatible mean 32.8% vs incompatible 34.3%; best compatible triple ranks 9th. Third family, now at multi-scenario strength.
- **No controller performs worse than baseline in any of the 1,250 runs**; worst active triple still averages −21.3%.

**Headline claim now validated at the same evidentiary standard as the other two families: best generated controller achieves 43.1% ± 4.1 mean energy reduction across 10 scenarios, vs 13.6% (single-scenario) for the hand-coded reference.**

**Timing (power-instrumented runs are ~80× costlier than plain-Datacenter runs).** Steady-state mean 45.25 ms/run (median 44.25, p95 57.5; max ≈330 ms is row-0 JIT warmup, same shape as the cloudlet family's timing run), flat across scenarios and module variants. The cloudlet family ran at 0.61 ms/run — the difference is `PowerDatacenter`'s periodic per-`schedulingInterval` processing (energy interpolation every tick at interval 1, ~6k processing events per run) vs plain `Datacenter`'s sparse event-driven processing; the continuous makespan values confirm the small interval is in use. Consequences: 5×5×5-scale multi-scenario runs are trivially cheap (10 scenarios ≈ 1 min, 100 ≈ 10 min single-threaded), but the eventual 10,000-combo × 100-scenario mixed-pool sweep is ~12.5 h single-threaded for power-instrumented rows. Levers, in preference order: parallelise (runs fully independent — 8 workers → <2 h); raise `schedulingInterval` as a direct cost knob (≈linear speedup, at the price of reintroducing makespan quantization — measure before committing); fall back to 10 scenarios (~75 min) only if necessary.

#### Remaining caveat

The hand-coded reference's 13.6% is still a single-scenario figure; the 2.5× (now ~3×) generated-vs-reference gap compares a 10-scenario mean against it. Running the reference through the same 10-scenario harness is cheap and would make that comparison symmetric. 100-scenario strengthening and the mixed-pool sweep are optional extensions, not validity requirements.

### PowerDatacenter Timing Quantization — Makespan Snaps to 100-Unit Ticks (2026-07-03)

`simulation_results_energy.csv` (375-row cloudlet-migration-module sweep run against the power-instrumented scenario) showed every makespan value as an exact multiple of 100 (range 2100–6200), unlike the original non-power sweep's continuous decimal values (e.g. 3087.44). Confirmed via `df['makespan'] % 100`: 358/375 rows exactly `.00`, remaining 17 off by `0.01` float noise — not coincidental.

**Root cause**: `HollowedControl`'s own MAPE tick cadence (`observationRate=100`) is a separate mechanism from `PowerDatacenter`/`PowerVm`'s internal periodic scheduling tick, which in the current `PowerConstructor.java` is also set to 100. Plain `Datacenter` finalises `CLOUDLET_RETURN` at the exact continuous-time completion event; `PowerDatacenter` only finalises/reports completions at its own periodic tick boundary, snapping true finish times up to the next 100-unit mark.

**Confirmed as a genuine behavioural shift, not a display artefact.** Merged the 343 module-combinations common to both `simulation_results_cloudlet.csv` (orig, plain `Datacenter`) and `simulation_results_energy.csv` (power) on `(monitor,analyser,planner,executor)`:
- `opportunity_cycles` differs in 182/343 (53%), `actions_executed` in 191/343 (56%), `conversion_rate` in 236/343 (69%).
- Only ~50% of rows match a clean `ceil(orig_makespan/100)`, and 18% of combos finish *earlier* under power — rules out "same run, later rounding."
- Moderate correlation (r≈0.55) between makespan shift and opportunity_cycles shift confirms the causal chain: `observeAndAct()` self-reschedules every `observationRate` until `getCloudletList().isEmpty() && submitted==received` fires, so the number of MAPE ticks before termination tracks the (now-shifted) real completion trajectory, which itself shifts because the coarser datacenter tick changes when host/VM state updates land.
- `average_cpu_demand_variance` stays frozen at exactly 0.190647 across all 375 rows — confirms modules' decision logic is unchanged; only the clock granularity they're operating on differs.

**Open fix, not yet applied**: tighten `PowerDatacenter`/`PowerVm`'s internal scheduling interval back to 1 (matching the already-validated `PowerScenarioTest.java`) if/when numeric comparability to the non-power cloudlet-migration sweep is needed. Leave `HollowedControl.observationRate=100` untouched — that's the MAPE cadence, working as designed.

### Average Power vs Total Energy — Total Energy Is a Makespan Proxy, Not a Power-Efficiency Signal (2026-07-03)

Prompted by the question of whether a continuous (non-batch) workload framing should target average power rather than total energy. Computed `avg_power = total_energy / makespan` across all 375 rows of `simulation_results_energy.csv` (cloudlet-migration modules only, no VM migration):

- `total_energy` correlates with `makespan` at r≈1.0 — under this instrumentation, total energy is almost entirely a restatement of how long the run took, not a genuine power-efficiency measurement of the controller.
- `avg_power` across all 375 combinations is nearly flat: 668.40–668.64 W, a **0.036% relative spread** — every cloudlet-migration controller, including the fully inert (0 migrations) case, draws essentially the same average power.
- The residual signal is real but small: `avg_power` correlates -0.49 with `actions_executed` and -0.77 with `conversion_rate` (more migration activity → modestly lower average power), correctly signed but swamped by a near-constant floor.

**Likely mechanism**: the calibrated model is `PowerModelLinear(250, 0.6)` → `power = 150 + 100×utilization` per host. Even a fully idle host still draws its 150W static floor under this formula. Unless `PowerDatacenter`'s total-power summation explicitly excludes zero-VM hosts (rather than calling `getPower()` on all hosts regardless of occupancy), evacuating a host removes only its ≤100W dynamic slice, never its 150W static contribution — which would contradict the "empty host = 0W" ground-truth semantic already decided (Phase 1) but not yet confirmed as actually enforced at the summation level. **RESOLVED 2026-07-08 — empty host contributes 0W at the summation level, confirmed empirically.** In the random-scenario seed-42 anchor run (see "Mixed-Pool Preparation" below), a draw with one fully empty host reported total energy matching the hand computation `Σ_occupied(150 + 100×u) × makespan` almost exactly (968.75 W × 6256.01 ≈ 6.06M W·sec vs reported 6,059,531.25); an idle-but-on 150W floor for the empty host would have added ~940k W·sec. The "empty host = 0W" semantic is enforced end-to-end.

**Empirical confirmation via a second data point**: running the VM-migration family's load-balancing controller (objective: minimise cross-host CPU-demand variance) on the same power-instrumented scenario gave **Total energy = 6,779,386.1875 W·sec, avg_power = 1093.45 W** — a **~64% increase** (1093/668 ≈ 1.64×) over the cloudlet-migration ceiling. Mechanistically consistent: minimising demand variance requires spreading VMs across *more* hosts to equalise load, which is structurally the opposite of consolidation — every additional host carrying any VM pays its full static floor continuously. Cloudlet-migration never moves VMs at all, so it inherits whatever the initial First-Fit placement produced (incidentally more consolidated) rather than actively working against consolidation. Rough back-of-envelope against the calibration (`150+100u` per active host, 6 hosts max): 668W is consistent with only ~3–4 hosts carrying load continuously; 1093W is consistent with something close to all 6 hosts being kept warm.

**Reconciles both findings**: intra-host cloudlet scheduling barely touches `avg_power` (0.036% spread) because it never changes *which* hosts are powered; inter-host VM placement is where the real leverage is — but only if the objective favours consolidation. The existing VM-migration Planner does the opposite by design (load-balancing ≠ energy-minimising), giving a concrete empirical worst-case (1093W) alongside the near-inert cloudlet-only floor (668W) as bounds for the energy-aware Planner (Phase 3) to land between.

---

## Mixed-Pool Preparation — Random Scenario and Stage-0 Characterisation (2026-07-08)

### Next-Step Decision: Cross-Family Mixed-Pool Sweep

With all three families independently validated (QoS/VM-migration, throughput/cloudlet-migration, power), the agreed next step is the original Phase-6 design goal: a merged-registry sweep across all ~15 monitors × 15 analysers × 15 planners × ~7 executors, testing composition across action types (2- vs 3-tuple), indexing domains (host- vs VM-indexed), and objectives. Key design decision: **log all three ground truths (demand variance, makespan, avg power/energy) on every row regardless of family** — every controller then gets scored against all three objectives, yielding the empirical Pareto surface (generalising the r = −0.94 QoS/power opposition) without building the Fusion stage first. Fusion becomes a follow-up informed by real frontier data. Known prerequisites, all documented in prior sections: length-agnostic sentinel predicates, per-boundary compatibility booleans, per-run try/catch so cross-family type crashes log as FAILED rows (they are data, per the Executor2 ClassCastException precedent), and a scenario fair to all three objectives — which motivated everything below.

### Random Scenario — Design

**Problem:** both existing placement policies are structurally biased. First-Fit manufactures imbalance (QoS-friendly) but lands at/near the consolidation optimum (power-degenerate, confirmed by direct arithmetic); Least-Full manufactures spread (power-friendly) but leaves a load balancer almost nothing (baseline variance 0.007). Each is near-degenerate for one objective. Likewise the broker's default round-robin cloudlet binding is a hidden balancing heuristic — exactly 5 cloudlets per VM, workload variance only through length draws.

**Solution: two new seeded random axes**, each independently toggleable in the scenario file (`RANDOM_PLACEMENT`, `RANDOM_ASSIGNMENT`):

- **Random VM placement** — `SelectionPolicyCustomRandom` (uniform draw among non-excluded hosts, `Random` injected via constructor, returns null on exhaustion), plugged into `VmAllocationWithSelectionPolicy`'s exclusion-retry protocol. The **stock CloudSim 7.x `SelectionPolicyRandomSelection` is unusable**: it constructs an unseeded `RandomGen` per call (non-reproducible placement — breaks per-seed baselines and every exact-reproduction validation) and its retry loop spins forever when all candidates are excluded (silent-hang class, same as `schedulingInterval=0`). Third entry in the stock-component defect list alongside `SelectionPolicyLeastFull` and `getCompletedVms()`.
- **Random cloudlet→VM assignment** — `cloudlet.setGuestId(assignRng.nextInt(NUM_VMS))` at creation; broker round-robin only binds cloudlets left at guestId −1, so the toggle needs no broker changes.

**Seed discipline:** `lengthRng = new Random(seed)` exactly (preserves the historical seed-42 cloudlet population); `assignRng = new Random(seed ^ 0x9E3779B97F4A7C15L)` — derived sub-seed, because two `Random` instances with the *same* seed produce identical state sequences, structurally correlating lengths with assignments. Rule: **never change the seed of a stream whose draws are already published; freely derive sub-seeds for streams with no history.** (This is the correct generalisation of the `CLOUDLET_SEED_OFFSET` lesson, which was about the former.) Documented accepted deviation: the placement policy uses the *raw* seed (shares a stream with `lengthRng`) — anchors were minted against it before the correlation was flagged; acceptable for placement since the correlation is weak, but noted in the scenario source.

**Random drifts toward Least-Full, not First-Fit** (question raised, resolved by theory then confirmed by survey): uniform random has no ordering bias, so expected occupancy is spread-with-fluctuations — LF-like occupancy, FF-ish unevenness. Predictions: mean occupancy 5.33/6, P(host empty) = (5/6)¹² ≈ 0.11.

### Instrumentation: MeasuringBroker Reintroduced (family-agnostic)

The QoS-era `MeasuringBroker` (no control logic, observation only) extended rather than duplicated — same "existing type already covers this" principle. Changes: dispatch via the `processOtherEvent` hook instead of a hand-copied `processEvent` tag chain (inherit base dispatch, add exactly one behaviour); first-observation-cycle placement dump + occupied-host count (**measurement unconditional, only printing behind a `verbose` flag** — a triple bug where the capture sat inside the verbose gate with `placementDumped` initialised true produced −1 occupancy across two survey runs before being caught); a clock guard (`throw if CloudSim.clock() > 100,000`) converting any never-terminating run into a named exception — insurance against the silent-hang class. Division of labour: selection policy logs placement *decisions* (System.out, includes retries), MeasuringBroker logs confirmed *state*, creation-time histogram logs assignment; energy read externally from `PowerDatacenter.getPower()`.

### Three-Anchor Validation (seed 42) — All Published Constants Reproduced Exactly

| Config | Variance | Occupied hosts | Total energy (W·sec) | Matches |
|---|---|---|---|---|
| First-Fit + RR | 0.190647 | 3/6 | 4,090,075.0 | published FF constants, to the digit |
| Least-Full + RR | 0.007053 | 6/6 | 6,842,275.0 | published LF constants (avg power 1118.57), to the digit |
| Random + Random | 0.120334 | 5/6 | 6,059,531.25 | new anchor (makespan 6256.01) |

Reproducing the FF and LF constants to full precision validates the entire reconstructed pipeline (scenario file, MeasuringBroker, FixedPowerHost, policy chain) against the published datasets. Bonus finding: identical cloudlet finish tables across FF/LF/Random under round-robin — **makespan is placement-invariant when every VM receives its full MIPS grant** (the established non-discrimination result, observed directly), so makespan is an *assignment-axis* descriptor. A config-mislabelling incident (a run labelled Least-Full was actually First-Fit — caught because the energy matched the FF constant exactly) motivated replacing the boolean with explicit labels in all output; three placement policies cannot be expressed by one boolean.

### 100-Seed Survey (`PowerScenarioSurvey`, seeds 42–141, no controller)

Streaming per-row CSV (`scenario_survey.csv`: seed, placement, assignment, makespan, total_energy, average_power, average_cpu_demand_variance, occupied_hosts, max_vm_cloudlets). Results against prediction:

- **Occupancy {4: 8, 5: 47, 6: 45} — mean 5.37 (predicted 5.33), empty-host rate 0.63 (predicted 0.67)** — the policy is unbiased to within sampling noise. Occupancy↔avg_power r = 0.962, as the 150W-floor power model demands.
- **QoS headroom:** variance median 0.0573, range 0.0022–0.1507; 15/100 seeds QoS-thin (<0.02), 4 below the LF anchor — honest scenario diversity, absorbed by per-seed baselines. Max never approaches FF's 0.19.
- **Power headroom: 100/100 draws** (occupancy always > the packing minimum of 3). **Throughput headroom: 100/100 draws** (max cloudlets/VM 7–11 vs round-robin's uniform 5).
- **Makespan 3622–14623 — a 4× spread across seeds**, the strongest justification yet for the per-seed-baseline discipline; a fixed global baseline would be off by up to ~2× either way.

**Verdict: the random scenario is live for all three objectives in ~85% of draws and for power/throughput in 100% — validated as the mixed-pool scenario.** FF and LF remain the within-family scenarios for the published results; nothing run on the random scenario is comparable to prior datasets.

### Methodology Lessons (this stage)

- **Result CSVs are write-once artifacts; nothing that can save them ever opens them.** An Excel open-and-save silently rewrote numeric columns to scientific notation. View copies if needed; the canonical file is only ever produced by the harness. Contaminated files are regenerated, not repaired.
- **Mid-run snapshots of streaming CSVs are readable but truncated — check completeness (row count, last-line integrity) before diagnosing crashes.** Two apparent harness "crashes at iteration ~84" were investigated (leak hypotheses, forensics on partial final lines) before resolving as stale/incomplete file copies; the completed run does all 100 iterations without issue. The streaming-write discipline is what made partial files readable — a feature, but one that makes truncation look like well-formed data.

### Rollout Ladder (agreed)

- **Stage 0 — no controller, multi-seed characterisation: DONE (above).**
- **Stage 1 — single controller, single sim:** one hand-coded reference per family (QoS, throughput, power) against a random-scenario seed; question is only "does each controller class engage." Not started.
- **Stage 2 — multi controller, multi sim:** per-family 5×5×5 × 10 seeds on the random scenario (skip single-sim sweeps — no information a 10-seed run lacks), then the merged pool. Not started.

Discipline: never change the scenario and the module pool in the same step.

---

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

---

## Known Limitations and Environmental Constraints

- **`actionable_cycles` is a weak, near-degenerate gate.** Saturates to the ceiling (45/45) for 3 of 5 analysers (v1/v2/v3) regardless of monitor, in 66% of all 375 rows overall. It measures "an anomaly exists somewhere," not "a migration was possible" (which needs an OVERLOADED source *and* an UNDERLOADED destination co-occurring). Only analyser_v4 and analyser_v5 show real dynamic range. `opportunityCycles` (co-occurrence-based) was added 2026-07-01 as the more meaningful denominator for conversion rate — see "Actionable-Cycle Gate" and "Instrumentation Architecture" above.
- **Self-reported module counters were an unverified trust boundary.** `getActionableCycles()`/`getActionsExecuted()` were self-tracked inside 18 independently LLM-generated modules and never independently checked. Moved to controller-tracked (`HollowedControl`) computation 2026-07-01, removing this class of risk. The old interface methods remain on the modules, unused, to avoid regeneration risk.
- **planner_v5's declared GUID input contract does not reflect its internal logic.** It gates on the declared `LoadState[]` (cpu-util-loadstate) but scores candidate migrations using independently recomputed demand pressure (`getCurrentRequestedTotalMips`), ignoring the metric that produced the classification. Confirmed from source 2026-07-01. Reframed as "GUID-agnostic design" rather than mislabelling — see planner_v5 mechanism note above — but it does violate this project's own stated rule that a planner's inputGuid should be an active semantic constraint on its internal logic, and is worth flagging as a limit on that rule's generality.
- **Module-quality conclusions can be ground-truth-metric artifacts.** planner_v5 was characterised as a "systematic failure" under the deprecated cpu-util ground truth and turned out to produce the best-in-dataset result (90.2% improvement) once the ground truth was switched to demand pressure. Its behaviour (conversion rate) never changed — only the metric used to judge the outcome did. Any per-module ranking or characterisation should be treated as provisional until cross-checked against an independent/corrected ground truth. This is now a documented methodological finding, not just a data-quality footnote.
- **Ground truth metric — resolved.** Switched from `getTotalUtilizationOfCpuMips` (scheduler-timing-noisy, structurally unbounded — confirmed via CloudSim source, see "CloudSim Observability Constraint") to `getCurrentRequestedTotalMips` (bounded ≤1.0 by construction, per `VmSchedulerTimeShared`'s no-oversubscription guarantee). New baseline: 0.222222. Old baseline (2.687) is invalidated and retained for historical reference only.
- **makespan non-discriminating** — VM migration redistributes load but cannot create MIPS. In a time-shared scheduler, cloudlets complete at the same wall-clock time regardless of migration decisions. `groundTruthAvgVariance` is the primary performance metric.
- **`isSuitableForGuest` semantic gap** — checks provisioned capacity, not runtime utilisation. Causes livelock when UNDERLOADED hosts are provisioned full. Correct planner pattern: utilisation-based destination selection first, `isSuitableForGuest` as final guard, iterate candidates on rejection.
- **`host.getAvailableMips()` does not exist** in this CloudSim build. CPU utilisation must be computed by iterating `host.getGuestList()` and summing `vm.getTotalUtilizationOfCpuMips(now)`.
- **RAM and BW signals are static** — CloudSim allocates RAM and BW statically. Excluded from the active GUID family.
- **monitor_v1 and monitor_v2 measure distinct signals** — cpu-util (`getTotalUtilizationOfCpuMips`) fluctuates with scheduling timing and can read 0.0 mid-run; cpu-demand (`getCurrentRequestedTotalMips`) is stable and deterministic. They are not API-equivalent. See CloudSim Observability Constraint above.
- **Single datacenter** — deliberate constraint for the generation experiment, not a framework limitation.
- **Executor slot has minimal variation space** — all correctly generated executors are functionally equivalent under the `int[]{vmId, hostId}` contract.
- **Module regeneration risk** — modules lost when not committed to version control. Regenerated modules under the same spec may differ from originals.
- **`vm.getCurrentRequestedTotalMips()/vm.getMips()` is a structurally degenerate ratio, not a usable demand signal.** Confirmed via diagnostic logging (2026-07-03): reads exactly `1.0` for every VM, every cycle, in the cloudlet-migration scenario — undersubscribed hosts + `VmSchedulerTimeShared`'s no-oversubscription guarantee mean any placeable VM is unconditionally granted its full rated MIPS regardless of actual cloudlet workload. Stronger case than the earlier cpu-util retirement (noisy vs. a proven hard constant). `demand` retired from the cloudlet-migration metric vocabulary; `getCurrentRequestedTotalMips()` removed from the approved API. Any Planner feasibility check built on this ratio (e.g. `planner_v1`'s `hasCapacity()`, threshold 0.85) is unconditionally unsatisfiable and will never propose a migration.
- **`MIPS_TIERS`/`PeProvisionerSimple` mismatch causes deterministic VM placement failure.** A VM MIPS tier value exceeding the per-PE provisioning cap (e.g. tier `2000` vs. `PeProvisionerSimple(1000)` with `pesNumber=1`) is structurally unplaceable — not a capacity-exhaustion failure, a single-PE cap violation for that VM's *entire* request. Deterministic given a seeded RNG (affects the same ~1/3 of the VM population every run). Fixed 2026-07-03 by lowering the offending tier to 250; any future tier changes must stay ≤ the per-PE MIPS cap.
- **`CloudletSchedulerTimeShared` has no hard rejection condition analogous to host-VM placement.** Unlike `HostEntity.isSuitableForGuest`, there is no cloudlet-migration equivalent that can reject a migration for inadequate destination-VM capacity — a VM always accepts another cloudlet, just time-shared more thinly. Any Planner "adequate resources" check is answering a question the scheduler doesn't structurally enforce; count/remaining-length-based heuristics are the correct substitute for a hard capacity gate, not a workaround for a missing one.
- **Open-ended Planner feasibility-check wording converged identically across independently generated variants.** All 5 cloudlet-migration planners implemented the same (broken) demand-ratio feasibility check from the same loosely-worded spec instruction ("must check whether destination has adequate resources"), despite independent generation. Vague constraints can produce a shared blind spot rather than genuine diversity — a methodological finding distinct from, but related to, the "GUID compatibility does not predict performance" line of results.
- **Cloudlet lookup by ID is VM-scheduler-scoped and has a destructive/non-destructive split.** `CloudletScheduler.getCloudletExecList()` is a safe, non-destructive peek; `cloudletCancel(int)` removes the cloudlet as a side effect and must not be used for resolution, since the Datacenter's own `processCloudletMove` handler performs the authoritative cancel exactly once when the migration event actually fires.
- **Cloudlet-migration family's inherited ground-truth column is structurally dead.** `average_cpu_demand_variance` (carried over unmodified from the VM-migration family) is host-level variance of per-VM demand pressure. Cloudlet migration never changes VM-to-host placement or VM MIPS ratings, so this metric cannot move under any cloudlet-migration action — confirmed 2026-07-03: identical to 15 significant figures across all 375 permutations of the corrected re-run. `makespan` is this family's actual discriminating metric (106 distinct values, 1927.04–6149.35) and matches the family's own stated goal. The ground-truth column needs replacing before it's used for anything, including cross-family/mixed-pool comparison.
- **`PowerDatacenter` with `schedulingInterval=0` hangs indefinitely rather than erroring.** Its update loop re-enters a "cancel and reschedule" branch whenever `cloudletSubmitted == CloudSim.clock()`; a 0-length interval reschedules at the identical simulated timestamp forever, so the clock never advances and the real per-cycle processing (energy accounting, cloudlet completion) is never reached. Symptom is a live, silent process producing no further output after cloudlets are submitted — not a crash, must be killed manually. Use a small positive `schedulingInterval` (e.g. 1) instead.
- **CloudSim 7.0 library bug: `HostDynamicWorkload.getCompletedVms()` flags every ordinary VM for removal after one cycle, regardless of real cloudlet completion.** Its `guest.getNumberOfGuests() == 0` check (added for 7.0's nested-container support) is unconditionally true for any VM not itself hosting containers — which is every VM in this project — so it's a false-positive removal trigger independent of actual remaining cloudlet length. Confirmed by source inspection against the `7.0` tag; not something fixable from calling code. Workaround: a narrow `PowerHost` subclass overriding only `getCompletedVms()` to drop that condition, keeping the correct `getCurrentRequestedTotalMips() == 0` check.
- **CloudSim 3.0-era documentation (the version search results default to) does not reliably describe CloudSim 7.x behaviour.** Confirmed concrete drift: entity-abstraction interfaces (`HostEntity`/`GuestEntity`/`PowerHostEntity`/`PowerGuestEntity`) are new in 7.x and don't exist in 3.0; `PowerHost.getPower(double)` changed from `protected` to `public`; `getMaxPower()`/`getEnergyLinearInterpolation()` moved from concrete `PowerHost` methods to default methods on the `PowerHostEntity` interface. Always verify against the actual pinned source (`github.com/Cloudslab/cloudsim`, matching tag) before citing specific CloudSim API details.
- **`SelectionPolicyLeastFull` (stock CloudSim 7.x) is a no-op for initial VM placement.** Its `PowerHost` branch ranks candidates by `1.0 - getUtilizationOfCpu()`, but at initial `VM_CREATE` time no cloudlet has executed anywhere yet, so every host reads exactly 0 utilization — a universal tie that a strict `>` comparison never breaks, so the policy always returns the first not-yet-excluded host in list order, reproducing First-Fit exactly. Confirmed via direct source read and a disproof trace (a genuine Worst-Fit policy could not have placed VM1 back onto Host0 given the state after VM0's placement, yet it did). Fixed locally via a custom policy (`SelectionPolicyLeastFullByCapacity`) using the class's own non-`PowerHost` branch (`getGuestScheduler().getAvailableMips()`, a static/provisioned-capacity signal) instead. The lesson generalises: utilization-based "fullness" signals are only meaningful once workload is already running; they're the wrong tool for one-time initial-placement decisions, structurally the same category of mistake as reusing a demand-based ground truth across families where it doesn't apply.
- **First-Fit placement (`VmAllocationPolicySimpler`) is a poor scenario for testing a power/consolidation objective.** Confirmed via direct arithmetic that the standard 12-VM scenario's First-Fit placement is already at or very near a local optimum for power (zero-headroom saturated host, and the one remaining host's free capacity smaller than the only VM that could conceivably move) — no feasible migration can improve on it. First-Fit was deliberately chosen to create imbalance for the QoS/load-balancing family, but "fill early hosts before touching later ones" is itself a crude consolidation heuristic, making it close to a best-case rather than worst-case starting condition for power. The power family needs the Least-Full/capacity-fix scenario (see "Hand-Coded Power-Minimizing Planner" above) to have genuine headroom to demonstrate anything.
- **`Planner5`'s single-shot destination selection caps how much consolidation gets captured, confirmed empirically not just theoretically.** With no fallback to a second-best destination when the top (most-loaded) candidate fails `isSuitableForGuest`, the planner can stall for the rest of a run even when other BALANCED hosts still have room — directly observed via before/after VM-allocation snapshots (see above). Left unfixed deliberately, as a candidate axis of variation for the LLM-generated batch (do independent generations discover a retry/fallback approach, or replicate this exact limitation — mirroring the cloudlet-migration family's "vague constraint, convergent shared blind spot" finding).
- **RAM/BW metrics are currently degenerate, not just unimplemented.** Every VM in the current harness is hardcoded to identical `ram`/`bw` values regardless of MIPS tier, so any RAM/BW-based host ratio is currently an exact proxy for VM count, carrying no independent information. Would need heterogeneous RAM/BW tiers (mirroring `MIPS_TIERS`) before being a meaningful signal — see "RAM/BW as Additional Metrics" above.
- **`vm.getCurrentRequestedTotalMips()` measures VM placement, not VM activity — confirmed from `CloudletScheduler.java` source (2026-07-07).** It sums `currentMipsShare`, set from the `mipsShare` parameter the host-level `VmScheduler` passes into `updateCloudletsProcessing()` — the allocated capacity share, not anything derived from the cloudlet exec list or `UtilizationModel`. Under `VmSchedulerTimeShared`'s no-oversubscription policy this value is granted in full the moment a VM is placed and never falls, even after every cloudlet on that VM has finished (confirmed empirically: frozen for 2400+ time units past the last cloudlet's confirmed finish time). Consequence: `UNDERLOADED`/OFF classification in the power family can only ever fire for hosts with zero VMs assigned, never for hosts holding idle-but-present VMs. See "CPU Demand Metric Is a Placement Signal, Not an Activity Signal" under Power-Aware Family.
- **`CloudletScheduler.getCurrentRequestedUtilizationOfRam()`/`getCurrentRequestedUtilizationOfBw()` do not share the CPU-demand accessor's placement/activity conflation.** Both iterate `cloudletExecList` directly and call `cl.getUtilizationOfRam/Bw(time)` per cloudlet — correctly reading 0 for idle VMs and genuinely reflecting whichever `UtilizationModel` is active. Orthogonal to the separate "RAM/BW is currently degenerate" finding above (which is about hardcoded-identical VM footprints, not this mechanism); worth revisiting if RAM/BW is ever given heterogeneous tiers, since this pathway would be a genuinely live signal where CPU demand is not. Caveat added 2026-07-07: under uniform footprints + `UtilizationModelFull`, these accessors return the concurrent cloudlet headcount wearing a RAM/BW label — deliberately excluded from the power-family approved API for that reason.
- **Instrumentation carried across families fails silently — three confirmed instances, now a standing rule.** (1) Cloudlet family: inherited demand-variance ground truth structurally dead. (2) Power family: proposal predicate tested the cloudlet family's 3-element sentinel against 2-element actions — constant-61 `actions_proposed`, zero information, live across two full sweeps before being caught. (3) Power family: inherited OVER∧UNDER opportunity gate read 0 on 48 rows that executed migrations (this family acts on BALANCED hosts). Sentinels, gates, ground truths, and counter predicates must be re-derived per family; "it compiled and produced numbers" is not evidence of transfer.
- **`compatible` logged as a single AND across three boundaries hides which link failed.** A stale executor GUID (pre-rename `host-migration-pair`) zeroed the entire compatibility axis for a full 375-row power sweep before being noticed. Log per-boundary booleans (monitor→analyser, analyser→planner, planner→executor) going forward.
- **Cross-batch GUID vocabulary drift produces systematic incompatibility when spec documents disagree.** The power family's activity metric was named `host-cpu-activity` in the monitor spec but still "VM demand" in the analyser spec at generation time; the two batches self-assembled disjoint tokens for the same metric, making every activity pairing incompatible. Root cause was spec inconsistency (confirmed), not LLM label instability — but the lesson binds either way: pin exact GUID tokens in every spec document of a family, or cross-batch composability degrades. analyser_v2/v5's GUID strings were **manually corrected post-generation** (provenance note: published power-family compatibility figures depend on these hand-edited labels; 14/125 = 11.2% compatible after the fix).
- **Illusory variant diversity: LLM variants can differ only along environmentally unreachable dimensions.** monitor_v1/v2 (power family) differ solely by a clamp to [0,1] that `VmSchedulerTimeShared`'s no-oversubscription invariant makes dead code — byte-identical behaviour across all 125 pairings, from a docstring premise ("ratio can exceed 1.0") that is false under this scheduler. Distinct from API hallucination and metric invention; detectable only by knowing the scenario's structural invariants. Variant-count claims should be discounted for such collapses.
- **The GUID convention underspecifies aggregation semantics.** monitor_v3 (sum of per-VM activity) and monitor_v4 (max per-VM activity) declare the identical `host-cpu-activity` GUID at different scales (≤1.0 vs ≤0.25 here). A GUID-matched analyser can be calibrated to the wrong scale by a label-identical monitor. Same category as the LoadState underspecification finding: the label names the metric, not its statistics.
- **Stock CloudSim 7.x `SelectionPolicyRandomSelection` is unusable for reproducible experiments.** It constructs an unseeded `RandomGen` per selection call (non-reproducible placement) and its retry loop spins forever when all candidates are excluded (silent-hang class). Replaced by `SelectionPolicyCustomRandom` (constructor-injected seeded `Random`, exclusion filter, null on exhaustion). Third stock-component defect alongside `SelectionPolicyLeastFull` (initial-placement no-op) and `HostDynamicWorkload.getCompletedVms()` (false-positive VM removal).
- **Random(seed) instances with the same seed are identical streams, not independent ones.** Two generators seeded identically produce the same state sequence, structurally correlating whatever they generate (e.g. cloudlet lengths with VM assignments). Derive sub-seeds (`seed ^ constant`) for streams with no published history; never reseed a stream whose draws are already published — the two halves of the generalised `CLOUDLET_SEED_OFFSET` lesson.
- **Result CSVs are write-once; opening them in Excel rewrites numbers.** An open-and-save converted energy values to scientific notation. Canonical result files are only ever produced by the harness; contaminated copies are regenerated, not repaired. Corollary: streaming CSVs snapshot mid-run as well-formed-but-truncated files — verify row count and last-line integrity before diagnosing crashes from an uploaded/copied file.
- **Power-model choice determines which observability signal "wins" — metric–reward alignment is a confound in module rankings.** Placement-demand monitors outperform activity monitors in the power family (~70 W) because `PowerModelLinear` reads the allocation/placement pathway, so idle-but-present VMs still draw power and activity-based OFF classification hides the cheapest evacuation candidates. Under an activity-based power model the ranking would plausibly invert. Any "monitor X beats monitor Y" claim must state the reward pathway it was scored against.
