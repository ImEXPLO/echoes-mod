# Memory Echoes

**Your tools remember what they have done — and pass it on when you outgrow them.**

Memory Echoes is a tool *identity* mod, not a tool progression mod. Nothing here makes a pickaxe
stronger than vanilla. The goal is that after a few hours you stop thinking of it as *a* pickaxe and
start thinking of it as *yours*, because it has a history no other pickaxe has.

When you finally upgrade to diamond, you don't abandon that history. You hand it down.

---

## The three layers

| Layer      | Lifetime  | What it is                                                    |
|------------|-----------|---------------------------------------------------------------|
| **Memory** | Permanent | The tool's story — what it has broken, what it's drawn to, how long it has been at work. Never spent. |
| **Echoes** | Temporary | A resource formed from the durability the tool actually spends. Recall them to mend it. |
| **Traits** | Derived   | Passive behaviours the tool earns. Never stored — always recomputed from Memory, so they survive an upgrade automatically. |

> Memory tells the story. Echoes create the economy. Traits express identity.

**Repair is the smallest thing you can do with Echoes, not the point of the mod.** Recalling Echoes
always gives back less durability than the work that formed them, so a tool mended only by its own
past still wears out. This is not a Mending alternative.

---

## Try it in two minutes

Everything below is vanilla-friendly — no new blocks, no new items, no new GUI.

You start with **A Worn Journal**, left by someone who studied Echoes before you. It is short, and
it is the only thing that tells you the two actions a tooltip cannot.

**1. Mine, and read the tooltip.** After a handful of blocks any tool starts remembering. Hold
`F3+H` for the exact tally.

**2. Recall its Echoes.** Sneak and right-click *at the air* holding a worn tool. It mends itself,
and tells you something about what it just reached for.

**3. Inherit a legacy.** In an anvil — **the new tool goes on the left, the old one on the right.**

```
        ANVIL
 ┌─────────┬─────────┐        ┌──────────────────┐
 │  LEFT   │  RIGHT  │   ->   │      OUTPUT      │
 ├─────────┼─────────┤        ├──────────────────┤
 │  fresh  │ veteran │        │  fresh Diamond   │
 │ Diamond │  Iron   │        │  carrying the    │
 │ Pickaxe │ Pickaxe │        │  Iron's history  │
 └─────────┴─────────┘        └──────────────────┘
   (kept)   (consumed)              +5 levels
```

Vanilla refuses this pairing entirely; Memory Echoes doesn't. The preview shows the diamond pickaxe
already carrying the iron one's story — including any Trait it had earned. Type a name in the anvil
to carry the old one's name forward too.

> **Get the slots the wrong way round and nothing happens.** The anvil's rule is the same as it
> always was: the left slot is the item you keep.

<!-- TODO before release: replace the diagram above with a screenshot of the anvil showing
     fresh tool (left) · veteran tool (right) · inherited successor (output). -->

**Enchantments are not inherited.** Merge passes on history only; enchantments remain the anvil's
business, and a cross-tier merge consumes the old tool along with them. The preview warns you before
you commit.

### Seeding a storied tool

Memory only accrues from real survival work, so hand yourself a veteran rather than mining for an
hour:

```
/give @s minecraft:iron_pickaxe[custom_name='"Old Reliable"',memoryechoes:memory={blocks_mined:12043,affinities:{stone:11000,ore:900},awakened_at:-336000L},memoryechoes:echoes={available:9,progress:17}]
```

The negative `awakened_at` is the trick that makes age readable on a brand-new world — it places the
tool's first day before the world's own clock, so it reads as *"A line begun 14 days ago"* instead of
*"Awakened today"*.

Put that pickaxe in the **right** anvil slot and a fresh diamond pickaxe on the **left** to see the
whole idea in one action.

---

## Design guarantees

These are enforced, not aspirational — several are covered by tests in `src/test`:

- **Recall is always net-negative.** An Echo costs more durability to form than it returns.
- **Traits never touch durability.** That axis belongs to Echoes alone.
- **Traits never compete with vanilla enchantments.** No Efficiency, Fortune, Sharpness, Unbreaking
  or Mending equivalents — identity and small conveniences only.
- **An upgrade never revokes an earned Trait.** Inherited Memory is floored at the highest threshold
  the predecessor had already crossed.
- **Merge is inheritance, not fusion.** One predecessor, same tool class, never additive — so
  histories cannot be funnelled together or laundered.
- **Merge is enchantment-agnostic.** Enchantments remain entirely the anvil's business.
- **Memory is versioned** from the first commit, and never fails to load.

---

## Building

```
./gradlew build           # compile + assemble
./gradlew test            # invariant tests
./gradlew runClient       # launch the client
```

Requires a Java 25 toolchain. If the Gradle daemon is running on a different JDK, Gradle forks a
compiler worker — set your IDE's Gradle JVM to Java 25 to avoid it.

---

**Minecraft** 26.2 · **NeoForge** 26.2.0.32-beta · **Author** EXPLO · All Rights Reserved.
