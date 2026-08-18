# 01 — Goal, Architecture, GUID Convention, Prompting, Reference Modules, Dissertation Context

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

### GUID Redesign — Digit-Encoded Compatibility (SUPERSEDED 2026-07-09 — see "GUID Redesign — Simplified to Build-Safety Signal" below)

**Superseded same day.** Retained for the design reasoning captured along the way (particularly the range/type flag decomposition — three orthogonal bits generating 8 domains including two, `[-1,1]` and `{-1,0,1}`, that fell out for free rather than needing hand-enumeration — which remains a good idea *if* purpose-encoding is ever revisited). Not the active plan; see the simplified version below for what's actually decided.

**Problem identified at supervisor sync:** the string convention above conflates two independent axes under one label. `host-cpu-demand` simultaneously asserts (a) *this is a single `double[]`* — the actual Java-level structural contract downstream code depends on — and (b) *this specifically means CPU demand pressure at host granularity* — a semantic/purpose claim with no bearing on whether the code will run. Every "illusory diversity" and "GUID vocabulary drift" finding in this project (02/03/04-family files) traces back to this conflation: modules that are code-compatible but purpose-incompatible run anyway (by design), while modules that are purpose-aligned but happen to declare different tokens get marked incompatible despite being code-compatible. The two questions the project actually wants to ask — "will this compile and run" and "do these modules mean the same thing" — were never separable in the data.

**Decision: replace the string GUID with a digit-encoded integer**, where each digit position is an independent, orthogonal axis:

- **Family digit** — action-type/pipeline family the module belongs to (e.g. 6 = cloudlet migration, 7 = VM migration). Encodes the actual code-compatibility question: can this module's output type be consumed by the next stage.
- **Shape digit** — arity of the Monitor/Analyser output: 1 = single `double[]`, 2 = multiple `double[]` arrays. This is the literal Java-level structural contract, now explicit instead of inferred from a naming convention.
- **Per-array metric-identity digit(s)** — when shape = 2 (multiple arrays), one additional digit per array names which metric that specific array carries. Only meaningful once shape is known, so this is a variable-length suffix, not a fixed-position digit.

**Consequence for multi-metric controllers**: once a controller consumes N metrics rather than one, the function combining them into a single cost signal (`Cost = f(metric_1, ..., metric_N)` — the Fusion stage sketched in 05-scenarios.md's Mixed-Pool section) is **taken out of LLM scope and human-authored as part of the spec**. Rationale (implicit in the decision, worth stating explicitly): composing a coherent cost function across heterogeneous metrics is a different, harder task than labelling a single metric's identity, and the project's existing evidence (metric invention, GUID vocabulary drift under spec inconsistency) suggests this is exactly the kind of open-ended task where LLM generation degrades rather than adds value. The LLM continues to generate modules against a fixed, human-specified GUID/cost contract, not the contract itself.

**Status**: decided in principle, not yet specified in digit-position detail or implemented. Needs: an actual digit-position table (mirroring the naming-ruleset table above), a decision on whether family/shape digits are fixed-width or variable, and a migration plan for the ~15×15×15×7 module pool already generated under the old string convention (regenerate vs. mechanically translate existing GUIDs).

### GUID Redesign — Simplified to Build-Safety Signal (Decided 2026-07-09, Supersedes Above)

Revisited the same day the digit-encoded version above was sketched, after the design work exposed it as solving a problem this project's own data says doesn't exist: GUID compatibility has never predicted controller performance, in any of the three families, consistently — the single most-repeated finding across 02/03/04-family files. Continued investment in a more precise *purpose*-compatibility encoding (identity digits, human-maintained lookup tables, per-metric coupling) was solving for something the evidence says labels can't solve.

**Simplified design — two separate fields, deliberately decoupled:**

1. **Functional GUID** — small, human-defined, mechanical. Encodes only what plausibly predicts a genuine *runtime* failure. Nothing in this framework fails at compile time — every module compiles regardless of GUID, since the interfaces are fixed Java generics; failures surface at runtime (e.g. the `Executor2` `ClassCastException` from wiring a 2-tuple planner's output to a 3-tuple-expecting executor). Currently just the **family digit** (action-type: 2-tuple VM-migration/power vs. 3-tuple cloudlet-migration, etc.) — the only axis shown so far to cause an actual crash rather than merely a quality difference. Grown reactively, not speculatively: add a digit only when a stress-testing extension (host-failure, MIPS-scaling, dynamic cloudlet injection — see 05-scenarios.md) surfaces a genuine new crash class. A likely second candidate, not yet added: a **shape digit** for single- vs. multi-array Monitor output, since a shape mismatch can corrupt the positional-indexing invariant (`LoadState[]` ending up the wrong length) and cause a real downstream crash — but this is shelved along with the multi-metric work below until something actually needs it (see 04-family-power.md, "Multi-Metric Diagnosis Object").
2. **Self-label** — the LLM's existing free-text description of what it believes it's measuring (`host-cpu-demand`-style, unchanged from the original convention). Logged, never checked against anything, no governance or lookup-table needed. Purely observational — the vehicle for the "does the LLM self-label accurately" research thread (metric invention, honest labelling of novel outputs), now explicitly decoupled from anything the constructor trusts operationally.

**Recommendation**: log both fields side by side in results CSVs — turns self-label-vs-reality into a pure analysis question with zero operational stakes, rather than a mechanism the constructor depends on.

**Status**: decided, not yet implemented. This is the active plan; the digit-encoded/identity-table scheme above is retained for its reasoning but is not being built.

### Functional GUID — Finalised Digit-Position Table (2026-07-09)

Reactive growth (per the "grow only when a real failure class appears" principle above) converged the same day into a concrete 4-digit structure: **`<bridge><level><suffix, 2 digits>`**.

- **Bridge** (digit 1): `1` = M-A, `2` = A-P, `3` = P-E. Self-describing on sight, per the earlier bridge-prefix decision.
- **Level** (digit 2): `1`–`4` = datacenter / host / vm / cloudlet. Reuses the entity-level taxonomy from the shelved `Diagnosis` object (04-family-power.md, "Multi-Metric Diagnosis Object") as a single tag, not a 4-slot record — doesn't reopen that shelving decision. **Justification, confirmed 2026-07-09**: a level mismatch is fundamentally a *length* mismatch — an Analyser/Planner calibrated for one entity count (e.g. `numHosts`) fed an array actually sized to a different count (e.g. `numVMs`) either throws an index-related error immediately, or — if the two counts coincidentally match — silently misaligns index i's meaning without any crash at all. Same failure category as the P-E case below, one bridge earlier. At P-E, level reads as "the entity type the action's subject is" (VM migration/MIPS-scaling/scale-in/scale-out/PE-scaling are all level=VM; cloudlet migration is level=cloudlet), consistent with the existing "terminal GUID names the subject of the action" convention.
- **Suffix** (digits 3–4, 00–99): **load-bearing only at P-E.** Disambiguates action types that share both bridge and level — e.g. VM migration vs. VM MIPS scaling are both P-E, level=VM, `int[2]`, but semantically incompatible (a host-index field vs. a magnitude field): crossing them wouldn't necessarily crash, it could silently migrate a VM to a nonsense host, the same silent-corruption pattern as the level mismatch above. **At M-A and A-P, the suffix is unused/reserved** — level already captures the identified crash-risk axis at those two bridges, and metric identity was deliberately routed to the ungoverned self-label field instead (see above). Explicitly not to be repurposed for metric-identity codes just because the digits are free — that would quietly reopen the governance burden the self-label decision was meant to cut.

**Status**: this is the concrete implementation of "Functional GUID" above — decided, not yet built. Supersedes the "just a family digit for now" description in that section with the finalised shape.

### P-E Level Digit Dropped (Decided 2026-07-10, Supersedes Level Detail Above)

Revisited the "level varies 3/4 at P-E" line above and dropped it. **P-E format is now `3<suffix, 2 digits>` (e.g. `30`, `301`...), not `3<level><suffix>`.** M-A and A-P are unaffected — `1<level><suffix>` and `2<level><suffix>` stand as finalised.

Reasoning: at P-E, suffix is already the load-bearing field (one code per action type — VM migration, VM MIPS-scaling, cloudlet migration, etc.), and each action type is 1:1 with an entity level by construction — the Executor dispatches on the specific suffix code, not on a generic "check level, then branch" path. A level digit here would be re-stating what suffix already fixes, not adding an independent crash check. Contrast with M-A/A-P, where suffix is currently unused (one variation each) and level is the *only* field doing disambiguation — genuinely load-bearing there.

Second, sharper reason surfaced in discussion: retaining a decorative field is worse than neutral, not merely redundant. If the level digit is something the LLM (or convention) still has to fill in correctly despite carrying no operational signal, a mislabelling produces a false action-level "mismatch" that would flag a functionally correct GUID as invalid — a spurious failure with no corresponding real crash behind it. This is the same failure mode that justified retiring the digit-encoded identity-table scheme (GUID governance costs paid for no predictive power). A field must both predict a genuine runtime failure *and* be immune to producing false invalidations from a cosmetic slip; level at P-E fails both. Consequence for implementation: this digit should be templated/hardcoded at construction time, not left as something the LLM writes "for consistency" — removing any path for it to drift.

**Status**: decided, not yet built (tracks the parent table's build status). Supersedes the level-at-P-E clause in "Functional GUID — Finalised Digit-Position Table" above; that section's Bridge and Suffix bullets, and the Level bullet as applied to M-A/A-P, still stand.

**Implemented 2026-07-10.** First sweep under the new scheme (power family, `simulation_results_power.csv`, 125 rows) run and passed — see 04-family-power.md, "New GUID Scheme — First Implementation Sweep." Caveat noted there: this sweep is single-level/single-action, so it confirms the scheme doesn't break anything but hasn't yet exercised its actual discriminating purpose. Both parent sections' "decided, not yet built" status is superseded to **implemented** by this entry.

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

## New Action Type — VM MIPS Scaling (Proposed by Supervisor, 2026-07-09)

Supervisor flagged that the current action set (VM migration, cloudlet migration, VM creation) is missing a mid-run **VM MIPS capacity scaling** action — changing a VM's rated MIPS while it's running, rather than moving work between fixed-capacity VMs/hosts.

**Effect on makespan**: direct and expected — more MIPS finishes a VM's assigned cloudlets faster.

**Effect on energy — confirmed non-obvious, two separate mechanisms:**
1. **Direct, via the power model's utilisation term.** `PowerModelLinear` prices host power off aggregate allocated MIPS share. Per the placement-vs-activity finding in 04-family-power.md, `VmSchedulerTimeShared` grants a VM its full rated MIPS the moment it's resident, regardless of whether it has active cloudlets. A VM's MIPS rating is therefore a *standing* contributor to host utilisation and power draw — raising it increases host power immediately, independent of makespan.
2. **Indirect, via makespan.** Faster completion shortens the time hosts stay powered — the same temporal-compression lever already quantified in 03-family-cloudlet.md (~51% of the cloudlet-migration family's energy win was this, with zero VM movement).

**Caveat to flag before building this action type**: because the power model's signal is placement-based, not activity-based, a MIPS-scaling controller can inflate a host's *reported* power by raising a VM's rated capacity while that VM is idle — no real work is being done, but the model prices it as if there were. This is the same conflation already root-caused for `Analyser9` (04-family-power.md). Any energy evaluation of a MIPS-scaling action needs to account for this before the numbers can be trusted.

**Status**: implemented and verified working (2026-07-10), then found to be built on an unresolved bypass — see below and 04-family-power.md, "VM MIPS Scaling — Built, Root-Caused, Fixed, and a Deeper Bypass Found," for the full experiment trail.

### VM MIPS Scaling — Implementation, Root Cause Fix, and a Deeper Design Problem (2026-07-10)

Built as `monitor_v4`/`analyser_v2`/`Planner6`/`Executor6` plus a new `ActionSpace.requestMipsScaling(GuestEntity vm, double newMips)`. First implementation (`vm.setMips(newMips)` alone) was a **complete no-op** — makespan and energy bit-identical across runs despite MIPS values visibly changing in the log. Root-caused via full CloudSim source trace: `Vm.getCurrentRequestedMips()` only reads `getMips()` while `isBeingInstantiated()` is true (essentially t≈0); after that it delegates to `CloudletScheduler.getCurrentRequestedMips()`, which just echoes back `currentMipsShare` — seeded once at initial host allocation and never independently re-derived from `vm.getMips()` again. `setMips()` changes a value nothing downstream reads. **Fix**: `requestMipsScaling` calls `vm.getCloudletScheduler().updateCloudletsProcessing(now, newMipsShare)` directly, forcibly reseeding `currentMipsShare`. Verified working: makespan 6200.01→5800.01, energy 6,824,375→6,376,875 W·sec on the original `{250,500,1000}`-tier/60-cloudlet scenario.

**The fix works by bypassing the exact mechanism that should have capped it.** `updateCloudletsProcessing()` writes straight into the VM's own `CloudletScheduler`, never through `Host`/`VmScheduler.allocatePesForGuest()` — the only code path that checks a request against the host's real per-PE cap (`if (mips > peMips) return false` in `VmSchedulerTimeShared`) and decrements the host's actual `availableMips`. Confirmed empirically (2026-07-10, `{250,500,1000,3000}`-tier/36-cloudlet follow-up experiment — full numbers in 04-family-power.md): VMs *born* demanding 3000 MIPS on their single PE (`pesNumber=1`, host PE cap 1000) are silently rejected at creation and never appear in the VM Allocation log at all, holding zero cloudlets for the entire run — yet `Planner6`/`Executor6` scaling an *already-running* VM to that same 3000 MIPS mid-run succeeds without complaint and visibly changes its processing rate. The same value is impossible through the front door and trivial through the fix's side door. This is a genuine architectural finding, not just a bug to patch quietly: any MIPS-scaling result obtained through the current `requestMipsScaling` at tiers above the host's per-PE cap is potentially exploiting a scheduler/executor consistency gap rather than testing a physically meaningful capacity increase.

**Two distinct, not-yet-separated action types fell out of chasing this.** "Maximise a VM's MIPS rating within its current PE allocation" (bounded by construction — a single PE can never legitimately exceed `peMips`) and "increase a VM's PE allocation" (the only legitimate way past that ceiling, claims a whole additional PE from the host's pool, can genuinely fail) are mechanically different operations the current single `requestMipsScaling` conflates. A corrected implementation needs to route the request through `Host`/`VmScheduler.allocatePesForGuest()` (so it's capped and can fail against real host capacity) rather than `CloudletScheduler.updateCloudletsProcessing()` directly, and — per `CloudletScheduler.updateCurrentCapacity()`'s `capacity /= Math.max(pesInUse, cpus)` — the PE-count lever only pays off when a VM has multiple cloudlets genuinely concurrent; it is a no-op for serial (one-at-a-time) VMs regardless of how many PEs are granted. **Not yet rebuilt**; current implementation stands as a documented-but-unresolved bypass. See 04-family-power.md for the full experiment and the static-floor/makespan confound it also surfaced, and 07-limitations.md for the itemised trap-list entries.

### VM MIPS Scaling — Design Resolved: Cascading Single Action + Argmax Selection (2026-07-10)

**Superseded 2026-07-13** — decision 1 below (one action, three-tier cascade hidden inside `ActionSpace`) is reversed; see "VM MIPS Scaling — Split Into Separate Actions, Cascade Rejected" below. Decisions 2 (argmax-ETC selection) and 3 (timeSaved worth-doing gate) stand, generalised to apply per action rather than per cascade tier.

Follow-on design session resolving the bypass/PE-cap problem above into a concrete build plan. Three decisions:

**1. One action, three internal fallback tiers — not two action types.** Considered splitting "maximise MIPS within current PE allocation" from "increase PE allocation" into two Planner/Executor pairs, then rejected it: under this project's one-action-type-per-controller convention, a controller wired to only the first would have no recourse once it saturates (exactly what the 3000-tier experiment kept hitting), and a controller wired to only the second couldn't handle the common case. Resolution — keep it as the single existing `requestMipsScaling` action/GUID, with the mechanism split hidden inside the `ActionSpace` implementation as a cascading fallback, attempted in order, stopping at first success:
   1. Raise the per-PE rate within the VM's current PE allocation, up to `peMips` (the real fix for the bypass documented above).
   2. If already saturated, claim an additional PE on the *same* host, if one is free.
   3. If the current host has no free PEs at all, migrate the VM to a host that does, then claim the PE there.

   This is consistent with the existing "Executors/Planners deal in intent, `ActionSpace` hides CloudSim mechanism" principle, and it sidesteps the single-action-per-cycle constraint entirely — the migrate-then-allocate tier fires as part of executing the one action already chosen this cycle, not as a competing action needing its own cycle. Tier 3 carries a real cost worth gating on, not treating as free: `VmSchedulerTimeShared` imposes a genuine 10% MIPS penalty on a migrating-out VM and grants only 10% to a migrating-in VM during the move, so it should only trigger when tiers 1–2 are truly exhausted, not as an eager first choice. If all three tiers fail (no spare capacity anywhere), `requestMipsScaling` should report a clean no-op rather than silently doing nothing, so the Planner/logs can distinguish "couldn't be scaled" from "didn't need scaling."

   Confirms, separately, that migration by itself contributes nothing to makespan (already logged in 07-limitations.md: "VM migration redistributes load but cannot create MIPS") — it only has value here as an enabler for the scaling action that immediately follows it in the same fallback chain, never as a standalone lever.

**2. Selection rule replaced: argmax(ETC), not relative/quantile classification.** The existing `analyser_v2` (median/MAD outlier test) answers "is this VM unusual compared to its current peers" — a load-balancing question, not the makespan question this controller actually needs answered ("which VM currently gates the overall finish time"). Since ETC already is the direct measure of that ("time until this VM's queue drains at current rate"), the correct classifier is a single-winner rule: **OVERLOADED = the not-yet-maxed VM with the maximum ETC**, full stop — no population statistics involved. A quantile variant ("top 25% by ETC") was also considered and rejected: it inherits the same population-thinning ratchet (finished VMs sit at ETC=0, never in the top slice, so once fewer than 25% of the fleet is still active every remaining active VM is trivially "top 25%" regardless of absolute need), and adds a new problem on top — a fixed quota that doesn't track whether the underlying distribution actually has one outlier or several, forcing arbitrary picks among near-tied VMs or ignoring genuinely-equal stragglers past the quota cutoff. Median/MAD and top-k both fail for the same underlying reason: they're relative-to-current-population rules answering a "how unusual" question, when the actual need is a direct "which one is the bottleneck" ranking with no population statistics at all.

**3. Worth-doing gate formalised: threshold on projected time saved, not raw ETC.** Rather than requiring `ETC > K` for some arbitrary `K`, gate on the actual payoff of the specific tier jump under consideration: `timeSaved = remainingMI × (1/currentMips − 1/nextTierMips)`, act only if `timeSaved > observationRate` (100 in this harness — the MAPE cycle length, chosen because a gain smaller than one cycle isn't even observable as a difference before the next decision point). This is a better floor than a raw-ETC threshold because it weights actions by how much the specific jump actually buys back (a 250→500 jump and a 1000→3000 jump are very different value propositions for the same ETC), and it auto-suppresses the VM6-style waste case (scaling a VM the same tick its last cloudlet finishes) without a separate special-case check — tiny `remainingMI` produces tiny `timeSaved` regardless of the mips ratio.

**Status**: designed, not yet implemented. Concrete build list for next session: (a) rewrite `analyser_v2` to emit `OVERLOADED` only for the argmax-ETC, not-yet-maxed VM, gated by the `timeSaved`/`observationRate` floor; (b) rewrite `requestMipsScaling` with the three-tier cascade above, sourced from real `Host`/`VmScheduler` capacity rather than the current direct `CloudletScheduler` write; (c) re-run the `{250,500,1000,3000}`-tier/36-cloudlet scenario against the corrected implementation to see whether the counterintuitive power result (04-family-power.md) survives once the bypass is closed.

**Superseded 2026-07-13, item (b) only** — see below.

### VM MIPS Scaling — Split Into Separate Actions, Cascade Rejected (2026-07-13)

Reopened the previous day's cascade design against this project's own "Fusion via Selection, Not Combination" stance (below) and against the design-space inventory (00-INDEX.md), and reversed decision 1 above.

**Objection to the single-action cascade.** Hiding the three tiers inside one `ActionSpace.requestMipsScaling()` call is combination logic sitting below the Planner, invisible to GUID typing — the same pattern already rejected at the goal level by "Fusion via Selection, Not Combination." The existing ActionSpace-hides-mechanism precedent (e.g. migration's target-host selection via `VmAllocationPolicy`) covers interchangeable implementations of *one* choice. The three tiers are not that: they are mechanistically distinct, carry different failure modes and costs, and two of them already have independent standing elsewhere. PE-count scaling is already its own 00-INDEX.md candidate, verified on its own mechanistic grounds in 06-cloudsim-notes.md ("VM PE-Count Scaling — Verified Throughput Lever via Contention"). Migration is already a fully built, independently validated action type from the QoS/power families. Tier 3 ("migrate to a host with a free PE, then claim it") is therefore not new code under this reading — it is the existing migration action composed with PE-count scaling, not a third primitive.

**Revised action split**:
1. **MIPS rate scaling** — new action. Raises per-PE rate within the VM's current PE allocation, up to `peMips`. Bounded by construction, cannot fail. Own GUID, own Planner/Executor pair.
2. **PE-count scaling** — new action. Claims a free PE on the current host, checked against real `Host`/`VmScheduler` capacity via `allocatePesForGuest()` (can fail). Own GUID, own Planner/Executor pair. Directly fulfils the existing 00-INDEX.md "VM PE-count scaling" candidate rather than burying it as a hidden tier.
3. **Migrate-then-claim** — not a third action. Composition of the existing migration action with #2, decided above the level of a single action.

**Selection logic deliberately left open, not hardcoded.** No fixed if-saturated-then-else priority chain gets written into `ActionSpace`. Which action applies given current state is either (a) an ordinary per-cycle Planner choice within a single controller wired to both new actions, or (b) left open as a candidate for its own generation experiment — an LLM-generated Analyser/Planner that proposes among multiple typed action candidates, parallel to how analyser_v2's argmax(ETC) rule was arrived at rather than assumed upfront. Building (a) now does not foreclose (b) later, since the actions stay independently typed either way — this is the point of splitting them.

**Carried over unchanged from the 07-10 resolution**: decision 2 (analyser rewritten to argmax(ETC), single-winner, not relative/quantile classification) and decision 3 (worth-doing gate on projected `timeSaved` vs. `observationRate`) both still apply, now evaluated per action rather than per cascade tier — each action's `timeSaved` formula differs (rate-scaling's stays as derived above; PE-claim's differs since the capacity effect isn't a simple tier-MIPS ratio; a migrate-then-claim composition needs to net out the 10%/10% migration MIPS penalty noted above against decision 1).

**Status**: designed, not yet implemented. Build list, superseding item (b) of the 07-10 list above: (a) unchanged — rewrite `analyser_v2` to argmax(ETC), not-yet-maxed, gated by `timeSaved`/`observationRate`. (b) **revised** — build two `ActionSpace` methods, `requestMipsRateScaling` (bounded, tier-1-only, cannot fail) and `requestPeClaim` (checked against real `Host`/`VmScheduler` capacity, can fail), replacing the single cascading `requestMipsScaling`; migration composition reuses the existing migration action, invoked by Planner-level logic when a PE-claim fails locally, not by `ActionSpace` internally. (c) unchanged — re-run the `{250,500,1000,3000}`-tier/36-cloudlet scenario against the split implementation to see whether the counterintuitive power result (04-family-power.md) survives once the bypass is closed.

**Status update, 2026-07-14: both actions built, both closed out.** Item (a): `analyser_ETC2` rewritten to real-ceiling-gated argmax(ETC) (using `getHostCapacity`, not the nominal tier list — the tier-list version turned out to have its own bug, an infinite-retry loop, closed in the same pass). Item (b): built as `requestMipsScaling` (retained the original name; rate-scaling half only, clamped to `getHostCapacity(vm)`) plus a new, separately-named `requestPeScaling(GuestEntity vm)` for the PE-count half (naming diverged from the `requestPeClaim` sketched here — no functional difference). Migration composition (tier 3) remains unbuilt, still deferred, no change from the plan above. Item (c): re-run confirms the counterintuitive result does **not** survive — see 04-family-power.md, "VM MIPS Scaling — Bypass Closed, Confound Isolated and Confirmed." Full build narrative, including two further CloudSim-mechanism bugs found while building `requestPeScaling` (dead `Pe.status` under time-sharing; `allocatePesForGuest`'s replace-not-additive semantics), in 04-family-power.md, "VM PE-Count Scaling — Built, Two Real CloudSim-Mechanism Bugs Found and Fixed."

**Status update, 2026-07-15.** `requestPeScaling` renamed to `requestPeAllocation`, paired with a new symmetric `requestPeDeallocation` — see 04-family-power.md, "VM PE-Count Deallocation — Built, Naming Finalised, Two ReadSpace Promotions," for the build and the replace-semantics bug it also had. **Migration composition (tier 3) reasoning finalised, still not built**: confirmed it needs no new `ActionSpace` method — the existing, already-validated `requestVmMigration` plus `requestPeAllocation` are sufficient, composed across MAPE cycles rather than atomically (a migration this cycle changes `vm.getHost()`; `hostHasFreePe`/`getPeDemand` re-derive live off wherever the VM now sits, so the existing PE-allocation eligibility gate re-fires correctly next cycle with zero new code). Open question if this is ever built: migration-destination selection needs to evaluate *candidate* hosts before moving, which `hostHasFreePe(HostEntity)` (now host-parameterised, see 04-family-power.md) supports, but nothing currently lets a Planner *enumerate* candidate hosts — a `ReadSpace.getHostList()`-style accessor may be a prerequisite, not yet built or confirmed missing.

### Additional Action-Type Candidates (2026-07-09)

Explored while pushing the design-space direction (00-INDEX.md) — which action, if any, actually serves which of the three fixed goals, checked mechanistically rather than assumed.

**VM scale-in (termination)** — the mirror image of the existing (hand-coded, unvalidated) scale-out reference module; see "Completed Reference Modules" above for what scale-out already solved (the `vmCreationPending` / `getGuestList()` workaround for `DatacenterBroker` not natively supporting mid-run VM creation). Goal-mapping, reasoned from mechanism rather than assumed:
- **Scale-out serves throughput primarily.** `requestVmCreation(vm, targetDatacenter)` only takes a datacenter ID — placement is delegated entirely to the active `VmAllocationPolicy`, so the Planner can't target a specific host's imbalance the way migration can. Weak fit for load-balancing. Adds raw capacity, which generally shortens makespan. Touches power only indirectly, via the same temporal-compression mechanism already documented for cloudlet migration and MIPS scaling — and that's in tension with the spatial-concentration lever that's driven every real power win so far.
- **Scale-in serves power primarily** — direct move toward the 0-VM/0W state, the established consolidation lever. Could serve load-balancing too, if the Planner deliberately targets an overloaded host's VM rather than picking arbitrarily.
- **Open risk before building it**: what happens to a terminated VM's unfinished cloudlets? If silently dropped rather than reassigned/required to complete first, scale-in would trivially improve `groundTruthAvgVariance` and `avg_power` by deleting workload rather than managing it — a metric-gaming risk in the same category as the old cpu-util ground truth (a good-looking number not measuring what it claimed). Needs checking against CloudSim's VM-destruction semantics before implementation, not after.
- `ActionSpace` currently has no VM-termination method at all — genuinely new interface surface, not just a new Planner/Executor pair against existing plumbing.

**Vertical VM resource scaling — MIPS/RAM/BW/PE as one action family, sequenced by readiness.** Mechanically identical shape: `int[]{targetVmId, newValue}`, differing only in which CloudSim setter is called. Same family digit under the functional-GUID scheme; plausibly one generic Executor parameterised by resource dimension. But not equally *useful* yet:
- **MIPS** — ready now. Already wired into both makespan and the power model's utilisation term (see "New Action Type — VM MIPS Scaling" above).
- **RAM/BW** — gated, not parallel work. Currently allocated statically and excluded from any goal-relevant calculation (see 06-cloudsim-notes.md and 04-family-power.md, "RAM/BW as Additional Metrics" — degenerate, identical footprint per VM). Scaling a resource no goal metric reads would have zero observable effect. The RAM/BW *metric* work (00-INDEX.md, "Design-Space Direction," not-yet-added list) is a hard prerequisite, not a sibling task.
- **PE count** — now verified viable, distinct from MIPS (parallelism vs. raw per-PE speed). See 06-cloudsim-notes.md, "VM PE-Count Scaling — Verified Throughput Lever via Contention": genuinely affects throughput under PE contention, and the current scenario (`pesNumber=1`, multiple cloudlets per VM under time-sharing) is very likely already in the contention regime by default.

**Direct host power-state control (suspend/resume a host outright)** — would be a more direct power lever than anything currently in the action set, since every existing power result is achieved indirectly through VM placement/evacuation. Whether CloudSim's `PowerHost`/`PowerDatacenter` package actually exposes a clean suspend/resume mechanism is **unverified** — needs a source check against the pinned 7.x tag before assuming it exists, per this project's established discipline (`SelectionPolicyLeastFull`, `getCompletedVms()`, `SelectionPolicyRandomSelection` were all real defects found only by reading source).

**Resolved and built, 2026-07-27 — see 04-family-power.md, "Host Power-State Control."** `PowerHostEntity.setPowerModel(PowerModel)`/`getPowerModel()` confirmed to exist via direct source read, providing exactly the clean mechanism this candidate needed. `requestHostPowerDown`/`requestHostPowerUp` built on top of it (empty-host precondition, asymmetric instant-down/delayed-up timing, an inrush-power decorator model for the boot window). Initially built, then judged to add no real decision value under the uniform `PowerModelLinear` power model (its zero-utilization special case already makes "empty" and "off" identical), then reversed back to genuinely useful once the power model itself was upgraded to real per-tier SPECpower curves that don't share that special case — see the same 04-family-power.md section for the full reasoning trail.

**Weaker candidates, noted not pursued**: cloudlet admission control (rejecting/deferring rather than migrating — a load-shedding action, different character from the rest of the set) and cloudlet priority/weighting within a VM's scheduler (plausible but unverified whether `CloudletSchedulerTimeShared` exposes any weighting mechanism beyond the PE-count-driven fair share above).

---

## Meta-Controller (Deferred, 2026-07-09)

A higher-level controller layer, raised at the same sync and explicitly parked behind the GUID redesign and stress-testing work (see "GUID Redesign" above and 05-scenarios.md, "Stress-Testing Extensions"). Sketch: given the Controller Library (the N best-performing verified controllers already produced by the Final Controller Constructor — see 05-scenarios.md pipeline), a meta-controller would read current system state and the active goal, and select which of the N controllers to deploy. Proposed validation: change the goal mid-run, or inject the host-failure conditions from the stress-testing extension, and check whether the meta-controller correctly switches controllers in response. Effectively tests whether controller *selection* can be automated on top of the already-validated controller *generation* pipeline.

**Status**: deferred, not started as of 2026-07-09. **Structurally begun 2026-07-15** — see "Selector / Controller / ControlUnit — Structural Multi-Controller Support" below. The switching *logic* itself (goal-directed selection based on live state) remains deferred exactly as scoped here; what's now built is the plumbing that a real meta-controller would need to sit on top of.

### Selector / Controller / ControlUnit — Structural Multi-Controller Support (2026-07-15)

Triggered by wanting to test whether two independently-built controllers (MIPS rate-scaling, PE-count scaling) could compose usefully rather than being validated only in isolation — directly the Meta-Controller's "select among the Controller Library" concept above, minus the goal-directed selection logic, which stayed explicitly out of scope for this pass (see Status below).

**`HollowedControl` renamed to `Selector` and restructured from "owns one controller" to "owns a list, dispatches to whichever is currently selected."** The class already ran a single-controller MAPE loop (monitor→analyser→planner→executor) as a periodic `SimEntity` tick; the generalisation keeps that periodic-tick/broker machinery untouched and only changes what runs inside it.

**Core technical obstacle: `HollowedControl<M,D,A>` was generic over one controller's metrics/diagnosis/action types — incompatible with holding several controllers with different types in one list.** Resolved with a standard type-erasure-at-the-boundary pattern: a new non-generic interface **`ControlUnit`** (`observeAndAct(ActionSpace)`, `getName()`, plus plain-typed instrumentation getters) is what `Selector` actually holds (`List<ControlUnit>`); a new generic class **`Controller<M,D,A>`** (bundling one Monitor/Analyser/Planner/Executor plus the existing `imbalancePredicate`/`opportunityPredicate`/`actionProposedPredicate` machinery, relocated wholesale from `HollowedControl` rather than redesigned) implements it, keeping its own types fully internal. `Selector` itself dropped `<M,D,A>` entirely; its three pre-existing constructor overloads became generic *methods* instead (legal Java — a non-generic class can have generic constructors), each wrapping its args into a singleton `Controller` and delegating down to a new `List<ControlUnit>`-taking constructor. Every existing single-controller call site keeps compiling unchanged with a two-line fix (class name only — `HollowedControl broker` → `Selector broker`, constructor call name only), since the backward-compatible overloads' argument shape didn't change.

**Selection mechanism, deliberately minimal**: `Selector` holds a `selected` field (the currently-active `ControlUnit`) and calls `observeAndAct` on it alone each cycle — not on every registered controller. An early design considered having every controller expose a type-erased `isEligible()` (via the relocated `imbalancePredicate`) and picking "first eligible" each cycle; **rejected** as the wrong mechanism once articulated against what was actually wanted: selection should be the Selector observing system-level ground truth (energy, load-balance, eventually completion-rate) against the *active goal* and deciding whether to switch which controller is in charge — not each controller voting on its own local trigger condition, which has nothing to do with any goal and is structurally the same "hidden decision invisible to the layer above it" pattern already rejected for the MIPS-scaling cascade. `isEligible()`/per-controller diagnosis-based selection was dropped entirely; `Controller.observeAndAct()` stayed a single combined call (monitor→analyser→planner→executor) since nothing external needs to inspect a diagnosis mid-pipeline once selection doesn't depend on it.

**`Selector.updateSelection()` exists as a named, empty hook — not yet real selection logic.** Deliberately scoped out: the near-term use case is single-controller-per-run (confirmed 2026-07-15), so building the ground-truth-vector/`Goal`-representation/switch-rule machinery now would be speculative — same discipline already applied elsewhere (not generalising `getHostCapacity` to take a bare host without a forcing case). `PowerDatacenter.getPower()` was confirmed live/queryable mid-simulation (a running total, not a final-only value) but `Selector` currently has no reference to the `PowerDatacenter` object at all (`DatacenterBroker` only holds the datacenter's ID, brokers/datacenters communicate via CloudSim's event system) — reachability gap, not a liveness gap, to close if/when ground-truth-based switching is actually built.

**First real test (2026-07-15) used a hardcoded, one-shot, time-based switch** (`t≥1500` → second controller) as a smoke test of the mechanism, not of any switching logic — confirmed working cleanly and, notably, the resulting composed run beat both single-controller baselines simultaneously. Full result in 04-family-power.md, "First Multi-Controller Test — Rate-Scaling → PE-Scaling Switch Beats Both Single-Controller Baselines."

**Status: structural refactor complete and validated end-to-end. Goal-directed switching logic is explicitly out of scope** — confirmed 2026-07-15: "This is all we needed from this refactor, the ability to switch out controllers to test a multi action environment, switch logic is out-of-scope." `updateSelection()` remains a real, empty, documented extension point if that changes later; nothing about its absence blocks single-controller runs, which stay the near-term default.

### Fusion via Selection, Not Combination — Architectural Stance (2026-07-09)

Three same-day decisions resolve into one coherent position, worth stating explicitly rather than leaving implicit across three separate entries: this project's answer to "how do we handle multiple objectives" is **selection across many simple, single-objective controllers, not combination inside one complex controller.**

The three decisions: (1) a multi-metric controller's cost function combining several signals is human-authored, not LLM-generated (see "GUID Redesign" above); (2) the Meta-Controller (above) is explicitly scoped as selecting which of the Controller Library's N best-performing single-objective controllers to deploy, based on live state and the active goal; (3) the multi-metric `Diagnosis`-object work needed to support within-controller fusion has been parked (04-family-power.md, "Multi-Metric Diagnosis Object") specifically because it's redundant with (2), not because it's infeasible.

Consequence for the "push design space" direction (00-INDEX.md): design-space growth should mean **more metrics and more actions serving the existing three goals** (load-balancing, throughput, power — held fixed), generating more distinct single-objective controllers for the Controller Library, rather than growing any individual controller's internal complexity to juggle multiple goals at once. Breadth of simple controllers, not depth of complex ones.

---

## Goals vs. Mechanisms (2026-07-16)

Raised directly by a question mid-session: is load-balancing itself an appropriate terminal goal, or is it a mechanism in service of something else? Worth stating explicitly since it reframes how "load-balancing" should be read across every handoff file that reports it as a result.

**Working distinction adopted:** a *goal* is a quantity this project can point to as evidence of a good or bad outcome on its own terms — makespan/QoS, energy, and (see 04-family-power.md, "Cost — Identified as a Missing Objective") candidate cost. A *mechanism* is an action that changes system state in service of whichever goal is currently active, with no positive or negative value of its own. Migration, MIPS rate-scaling, PE-scaling/descaling, and VM deallocation are all mechanisms under this definition — none of them are things this project wants more or less of intrinsically; they're only good insofar as they move a goal metric in the right direction.

**Load-balancing specifically is a mechanism, not a goal, in this project's own terms.** 02-family-qos.md's entire 5×5×5×3 permutation sweep already establishes this empirically, independent of this session: its own ground-truth metric is host-demand variance, and "makespan: non-discriminating — identical across all 375 permutations" was already on record there. A mechanism that reliably improves its own target metric (variance) while never moving the terminal metric (makespan) most controllers ultimately answer to is the textbook shape of "instrument, not objective." This session's combined-controller result (04-family-power.md, "Combined Load-Balancer + Rate-Scaling Result") is the first case where load-balancing *did* move makespan — but only as an enabler: freeing host capacity so a different mechanism (rate-scaling) had somewhere to put a boost, not through any load-balancing effect in its own right. That result is consistent with, not a counterexample to, load-balancing-as-mechanism.

**Why keep testing it as if it were a goal, then.** Raised and answered directly mid-session: even where a mechanism produces no observable positive ground truth inside this specific CloudSim setup, that doesn't make it valueless as a design choice — load-balancing is a load-bearing philosophy in real networking/distributed-systems practice (spreading risk, avoiding hotspot failure modes, headroom for bursty demand) that this project's current goal set (makespan, energy, cost) doesn't fully capture. Kept in the action space and reported on its own terms for that reason, with the goals-vs-mechanisms distinction now made explicit rather than left implicit, so future results correctly read "load-balancer converges cleanly" as a mechanism-health finding, not a goal-progress finding, unless a goal metric is also shown moving alongside it.

**Practical consequence:** any handoff entry reporting a mechanism's own diagnostic (migration convergence, scaling success/failure, variance reduction) should be read as necessary-but-not-sufficient — the terminal question is always whether a goal metric (makespan, energy, eventually cost) also moved, and by how much relative to the others. See 02-family-qos.md for the note connecting this back to that family's original ground-truth findings.

---

## Host Failure — "Maximise Service" as a Fourth Goal, Reactivating Meta-Controller Selection (2026-07-20)

Proposed mid-session while scoping host failure (05-scenarios.md's original stress-testing pair, 2026-07-09): treat service continuity/availability as its own terminal goal — "maximise service" — sitting alongside makespan/QoS, energy, and cost (see "Goals vs. Mechanisms" above for the goal/mechanism distinction this slots into). Under this framing, a family of healing mechanisms (migrate all VMs off a failing host then let it fail, power the host down proactively, partial/priority evacuation, etc.) exist purely in service of that goal, mirroring how migration/rate-scaling/PE-scaling already serve makespan/energy/cost.

**This is not a new idea bolted on — it's the trigger case the Meta-Controller section already named and then parked.** The original 2026-07-09 Meta-Controller sketch proposed validating goal-directed controller selection by "chang[ing] the goal mid-run, or inject[ing] the host-failure conditions from the stress-testing extension, and check[ing] whether the meta-controller correctly switches controllers in response." `Selector.updateSelection()` has sat as a deliberately empty hook since the 2026-07-15 structural refactor specifically because the near-term use case (single-controller-per-run) gave no real case forcing goal-directed switching logic to be built. A "maximise service" goal, triggered by a host-health signal, is exactly that forcing case — the current four-controller rotation runs on a hardcoded schedule, not a live decision, so this would be the first genuine exercise of `updateSelection()`.

**Design shape, consistent with existing conventions, not yet built:**
- Host health is a `ReadSpace` primitive (e.g. `isHostFailed(host)`), not a Monitor-computed signal — a raw fact with no strategy variance, same category as `getPeDemand`/`getHostCapacity` (07-limitations.md, 2026-07-15 promotion precedent).
- Failure injection is a scenario-construction event, not a controller action — same category as dynamic cloudlet injection (05-scenarios.md), not `ActionSpace`.
- Healing response reuses the existing, already-validated `requestVmMigration` action for evacuation rather than requiring a new action type, unless the CloudSim mechanics check (04-family-power.md, "Host Failure — CloudSim Mechanics Pre-Implementation Check") turns up something needing new interface surface (e.g. an explicit host-recovery/re-registration call).
- Strategic diversity — the genuinely interesting design-space question — lives at the Planner level: multiple candidate healing strategies (evacuate-then-heal, power-down-and-abandon, partial/priority evacuation) sharing one GUID-compatible Monitor/Analyser, the same generation-experiment shape already used for QoS/throughput/power. This would be a fourth family if pursued at that scope.

**Status: proposed and scoped, nothing built.** CloudSim mechanics check (04-family-power.md) found `Host.isFailed()` exists but is functionally inert under this project's scheduler/placement/processing code — every actual consequence of a failure has to be defined and built by us, which if anything strengthens the case that healing strategy is genuine strategic surface, not a thin wrapper. Open decisions before building: scope (one hand-coded reference healing strategy first, vs. spec multiple Planner variants immediately), the "maximise service" ground-truth metric (candidates: fraction of cloudlets completed vs. lost, VMs evacuated vs. caught, or total service-downtime), and the failure-injection mechanism (seeded arrival process, mirroring cloudlet injection, vs. something else). See 00-INDEX.md for current priority.

### Host Failure Mechanism — Built and Validated, Closing Out the Above Scoping (dated 2026-07-27, built in an undocumented prior session)

**Status update: this is no longer scoped-but-unbuilt.** The full mechanism described as "proposed and scoped, nothing built" immediately above is now built, per direct inspection of the current `Selector.java` — this entry exists to close that gap in the handoff record, which had fallen behind the actual codebase by at least one full session. Exact build date/session not reconstructable from this write-up; dated to when this entry was written, not when the work happened.

**What's built, confirmed via source review this session:**
- **Failure injection**: `registerFailure(delay, hostId)` / `failureSchedule`, sampled per the `Simulation Complexity.md` design (`Expo(3000)` inter-failure time, target host `floor(Uniform(0, numHosts))`), scheduled to `Selector` itself (`HOST_FAIL` tag) the same way cloudlet injection already worked.
- **Failure state + processing halt**: `processHostFailure` marks the host failed (`Host.setFailed(true)`), deallocates every resident VM's PE share, and tracks exposed cloudlets (`exposedCloudletIds`) and failure start time (`failureStartTimeByHost`) for downtime accounting.
- **Repair timer**: `repairDurationDist` (`LognormalDistr(0.6, log(800))`, matching `Simulation Complexity.md`) samples a repair delay, scheduled via the same `schedule(getId(), delay, TAG, data)` self-event pattern — this became the template later reused for host power-up (see 04-family-power.md).
- **Unrecoverable branch**: `processHostRepair` rolls a fixed 20% (`UNRECOVERABLE_PROBABILITY`) chance the repair fails outright; on failure the host is marked permanently dead (`permanentlyDeadHostIds`) and evacuated rather than repaired.
- **Evacuation**: `evacuateHost` migrates every resident VM to the first suitable healthy host (`isSuitableForGuest`, excluding already-failed/already-claimed-this-batch hosts); VMs with no available destination have their cloudlets explicitly cancelled and counted as abandoned rather than silently stalling — directly closing the "nothing automatic happens to VMs/cloudlets on a host we force down" gap flagged in the pre-implementation check (04-family-power.md, "Host Failure — CloudSim Mechanics Pre-Implementation Check").
- **Admission blocking + deferral**: `admitCloudlets` checks the target VM's host before submission; cloudlets destined for a failed-but-recoverable host are deferred (`deferredCloudletsByHost`, `deferralStartTimeByCloudlet`) and flushed once repair completes; cloudlets destined for a permanently-dead host are abandoned immediately.
- **Blast-radius / outcome logging**: `numRealFailures`, `totalDowntime`, `currentFailedHostCount`/`peakSimultaneousFailedHosts`, `numCloudletsDeferred`/`numCloudletsAbandoned`/`totalDeferredWaitTime`, `getNumCloudletsExposedToFailure()` — all exposed as plain getters, giving the "maximise service" ground-truth candidates listed above (fraction completed vs. lost, downtime) a concrete data source rather than requiring new instrumentation.
- **A real completion-chain bug was found and fixed as part of this build**: `PowerDatacenter`'s own `VM_DATACENTER_EVENT` reschedule chain permanently dies if every host with outstanding cloudlets was simultaneously failed at some point (its internal `smallerTime` guard never resets), so cloudlets that finish mid-outage never get `CLOUDLET_RETURN` sent and the simulation hangs. Fixed by force-sending `VM_DATACENTER_EVENT` at the end of every `processHostRepair` call, unconditionally.

**Not yet done**: task-tracked as `#10, "Run per-family experiments and log findings"` — the mechanism is structurally complete and was validated for correctness (no crashes, invariants hold) but has not yet been run through the full multi-scenario/multi-seed sweep this project's other headline numbers are held to (see 00-INDEX.md, "Conventions"). No blast-radius/downtime numbers are citable yet — this section documents the mechanism, not a result.

### ActionSpace Surface — New Actions and a Return-Type Reversal (2026-07-27)

A full audit-and-hardening pass over `ActionSpace`, run against this project's standing "atomic actions only, no bundled/steering logic, verify against real source before implementing" discipline. Full detail in 04-family-power.md (host power-state) and 07-limitations.md (bugs caught during the pass); this section records the interface-level and architectural decisions.

**New atomic actions added**: `requestVmDestruction` (VM_DESTROY, mirrors the existing VM_CREATE pattern), `requestCloudletCancellation` (CLOUDLET_CANCEL), `requestRamScaling`/`requestBwScaling` (direct provisioner mutation, same category as the existing MIPS/PE-count actions — not event-scheduled, since `RamProvisioner`/`BwProvisioner` are plain objects Selector already holds a reference to, not `SimEntity`s), `requestCloudletPause`/`requestCloudletResume` (CLOUDLET_PAUSE/RESUME, confirmed natively supported by `Datacenter.processCloudlet` alongside CANCEL — not a new CloudSim capability, just previously unwrapped), and `requestHostPowerDown`/`requestHostPowerUp` (see 04-family-power.md, new section on host power-state control). `requestVmCreation` was rewritten from a raw `new PowerVm(...)`-requiring signature to `GuestEntity requestVmCreation(int tierIndex, int sizeTierIndex, int datacenterId)` — tier-coupled MIPS/RAM/BW/core-count (mirrors real fixed-instance-family SKU convention, matches how `SelectorMultiSim` already constructs VMs) with VM storage size deliberately kept decoupled-but-discretized (a continuous, placement-unconstrained size field would let a controller drive storage toward zero to cut cost with no corresponding downside — the same reward-hacking lens already applied elsewhere in this project, e.g. the destination-fallback non-fix). Considered and explicitly rejected: post-creation VM storage scaling (would reopen the same exploit unless equally bounded) and hidden multi-step "evacuate host" actions (rejected on the same "combination logic invisible to GUID typing" grounds as the 2026-07-13 MIPS-scaling cascade reversal, above).

**Return-type reversal: five methods changed from `void` to `boolean`, partially superseding the 2026-07-14 "stay void for cross-action-type consistency" decision (07-limitations.md).** `requestMipsScaling`, `requestPeAllocation`, `requestPeDeallocation`, `requestRamScaling`, `requestBwScaling` now return the real, already-computed success/failure value instead of discarding it. This is deliberately *not* applied uniformly across every action — the underlying architectural split is real, not cosmetic: methods that mutate objects `Selector` already holds a live reference to (`VmScheduler`, provisioners — plain Java objects owned by the `Datacenter` `SimEntity`, not `SimEntity`s themselves) can report a synchronous outcome and were silently throwing one away; methods that must cross into the `Datacenter`'s own authority via `send`/`sendNow` (migration, creation, destruction, cloudlet submission, power-state changes) genuinely cannot report a synchronous outcome — the real result is determined later by CloudSim's own event processing — and correctly stay `void`. Considered and rejected: making every action uniformly event-scheduled specifically to enable future per-action delay simulation — rejected as solving a hypothetical future need at the cost of destroying a signal (sync vs. async) that currently tells the truth about CloudSim's architecture, and as directly undoing the point of this same change (an event-scheduled method can't synchronously report success either).

**Executor-level consequence, deliberately minimal.** `Executor.execute()`'s own boolean return keeps its existing attempt-level meaning (an action was fired at all, not that it succeeded) — preserving the actionable-to-action conversion-rate metric exactly as documented in 07-limitations.md. A new opt-in default method, `Executor<A>.getSuccessfulActionCount()` (defaults to `0`), lets an individual Executor additionally expose a cumulative count of how many of its *outcome-level* actions actually succeeded, without requiring any change to existing Executor implementations. See 07-limitations.md for the itemised bugs this rewiring caught along the way.

---

### ActionSpace Surface — Host Power-Down Made Genuinely Destructive, Cloudlet-Level Actions Narrowed for Generation Scope (2026-08-04)

**`requestHostPowerDown` changed from a silently-refusing guard to an unconditionally destructive action, closing an inconsistency with every other destructive `ActionSpace` method.** Previously, calling it on a host with resident VMs simply no-opped (logged, refused, returned) — the only `ActionSpace` method with this shape. Every other destructive action (`requestVmDestruction`, host-failure evacuation) executes unconditionally and lets the harness clean up the consequences afterward, consistent with this project's "no graceful recovery, but no silent state corruption either" stance (see `requestVmDestruction`'s own design rationale above). The precondition guard has been removed; `processHostPowerDown` now destroys every resident VM via a shared helper (`strandAndDestroyGuest`, factored out of `requestVmDestruction`'s existing logic) before swapping the power model, with a dedicated counter (`numCloudletsAbandonedHostPoweredDown`) keeping this cause distinguishable from every other abandonment source. See 04-family-power.md and 07-limitations.md for the build detail and the bugs caught wiring the counter through `SelectorMultiController.java`.

**Cloudlet-level `ActionSpace` scope narrowed for future generation batches — implementations kept, advertised surface reduced.** Of the five cloudlet-level methods (`sendCloudlet`, `moveCloudlet`, `requestCloudletCancellation`, `requestCloudletPause`, `requestCloudletResume`), only `moveCloudlet` will remain in `0. System Context.md`'s advertised `ActionSpace` listing going forward. `sendCloudlet` has no legitimate rationale as a *reactive* MAPE action (a controller responding to observed state has no reason to submit new work) and is the confirmed root cause of two separately-documented bugs this project already spent real debugging effort on (the `planner_v4`/`executor_v1` cloudlet double-counting mismatch, and the `CloudSimShutdown` misdirection mismatch) — `executor_v1`'s retirement removes the one Executor that misused it, not the underlying risk for whichever Executor is generated next. `requestCloudletCancellation` was already listed as a weaker, not-pursued candidate in this file's original action-type inventory (see "Weaker candidates, noted not pursued" above). `requestCloudletPause`/`requestCloudletResume` carry a genuine structural deadlock risk under the current single-`ControlUnit`-per-run architecture — see 07-limitations.md's new entry for the mechanism. All four are being kept fully implemented in `Selector.java`/`SelectorNoLogs.java` rather than deleted, mirroring the precedent already set for `isHostSuitableForGuest` (real interface keeps it, for backward compatibility with modules that already call it; the System Context spec stops advertising it because `canMigrateGuestToHost` is the better check to steer new generations toward). Not yet executed: a grep for existing callers of the four before finalizing, and an explicit marker in `0. System Context.md` noting its `ActionSpace` block is now a curated subset, not a verbatim copy of the real interface.

---

