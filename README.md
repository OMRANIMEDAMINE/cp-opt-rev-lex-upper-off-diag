# OptRevLex — Connected Non-Isomorphic Graph Generation via Reverse Lexicographic Ordering and Upper Off-Diagonal Connectivity Constraints

> **Source code for the paper:**  
> *Reverse Lexicographic Ordering and Upper Off-Diagonal Connectivity Constraints: A Synergistic Approach for Connected Non-Isomorphic Graph Generation*  
> Mohamed Amine Omrani · Wady Naanaa  
>  Journal (under review) 

---

## Table of Contents

- [Overview](#overview)
- [Key Contributions](#key-contributions)
- [Repository Structure](#repository-structure)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
  - [Running a Single Instance](#running-a-single-instance)
  - [Reproducing the Full Benchmark](#reproducing-the-full-benchmark)
- [Configurations](#configurations)
- [Benchmark Results (Summary)](#benchmark-results-summary)
- [How It Works](#how-it-works)
  - [OptRevLex Symmetry Breaking](#optrevlex-symmetry-breaking)
  - [Connectivity Encodings](#connectivity-encodings)
  - [The Synergy](#the-synergy)
- [Scope and Limitations](#scope-and-limitations)
- [Citation](#citation)
- [Authors](#authors)
- [License](#license)

---

## Overview

Generating all **non-isomorphic connected graphs** in a given family is a fundamental combinatorial problem with applications in network design, molecular structure elucidation, and bioinformatics. Two challenges must be solved simultaneously:

1. **Symmetry (isomorphic redundancy):** Any permutation of vertex labels produces an isomorphic adjacency matrix. A naïve generator revisits the same unlabeled graph many times under different labelings.
2. **Connectivity:** Only connected graphs are meaningful in many applications. Enforcing connectivity *during* search — rather than filtering after — is critical for efficiency.

This repository implements a **Constraint Programming (CP) framework** that addresses both challenges jointly through a provably synergistic co-design:

- **OptRevLex** — an Optimized Reverse Lexicographic row-ordering constraint for symmetry breaking.
- **Upper Off-Diagonal Encoding** — an O(n) connectivity constraint requiring *no auxiliary variables*, provably complete under OptRevLex ordering for regular graphs.

The key result is that OptRevLex and the upper off-diagonal encoding are **structurally synergistic**: the off-diagonal encoding is provably complete *because* of OptRevLex's directional property, and this synergy is impossible to replicate under standard lexicographic (OptLex) ordering.

---

## Key Contributions

| # | Contribution | Details |
|---|---|---|
| 1 | **OptRevLex Symmetry Breaking** | Adapts Codish et al.'s OptLex optimization to the reverse lexicographic direction. Provably satisfied by all canonical adjacency matrices. Reduces redundant solution counts by up to **105×** vs OptLex at K₂₀(2). |
| 2 | **Path-Based Connectivity Encoding** | Distance-variable encoding inspired by XCSP3 Model 18. Sound and complete (no redundant solutions per adjacency matrix). Works with any row ordering. |
| 3 | **Upper Off-Diagonal Encoding** | O(n) constraints directly on adjacency matrix entries, **no auxiliary variables**. Provably complete under OptRevLex for d-regular graphs. Structurally incompatible with OptLex. |
| 4 | **Cross-Ordering Complementation Identity** | sols(Kₙ(d), OptLex) = sols(Kₙ(n−1−d), OptRevLex). Validated on 9 non-trivial pairs; enables dense-instance enumeration at no extra cost. |

---

## Repository Structure

```
.
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── GraphGenerator.java        # Main entry point
│   │       ├── OptRevLexConstraint.java   # OptRevLex row-ordering constraint
│   │       ├── PathConnectivity.java      # Path-based connectivity encoding
│   │       ├── OffDiagonalConnectivity.java # Upper off-diagonal encoding
│   │       └── BenchmarkRunner.java       # Runs all benchmark instances
│   └── test/
│       └── java/
│           └── ComplementationTest.java   # Validates complementation identity
├── results/
│   └── benchmark_results.csv             # Full experimental results (Table 1)
├── scripts/
│   ├── run_single.sh                     # Run a single K_n(d) instance
│   └── run_full_benchmark.sh             # Reproduce all paper results
├── pom.xml                               # Maven build file
└── README.md
```

> **Note:** Adjust paths above to match your actual repository layout.

---

## Requirements

| Dependency | Version | Notes |
|---|---|---|
| Java JDK | ≥ 11 | Tested with OpenJDK 17 |
| IBM ILOG CP Optimizer | 12.10 | Commercial solver — requires a valid CPLEX/CP Optimizer license |
| Maven | ≥ 3.6 | Build and dependency management |
| Ubuntu / Linux | 22.04 LTS | Tested platform; other POSIX systems should work |

> **CP Optimizer license:** IBM ILOG CP Optimizer is a commercial product. Academic licenses are available through the [IBM Academic Initiative](https://www.ibm.com/academic). The JAR (`cpoptimizer.jar` / `cplex.jar`) must be placed in `lib/` or added to your local Maven repository before building.

---

## Installation

```bash
# 1. Clone the repository
git clone https://github.com/OMRANIMEDAMINE/cp-opt-rev-lex-upper-off-diag.git
cd optrevlex-graph-gen

# 2. Place your IBM ILOG CP Optimizer JAR in lib/
#    (adjust filename to match your CPLEX installation)
cp /path/to/cpoptimizer.jar lib/

# 3. Install the CP Optimizer JAR into your local Maven repository
mvn install:install-file \
    -Dfile=lib/cpoptimizer.jar \
    -DgroupId=com.ibm.icu \
    -DartifactId=cpoptimizer \
    -Dversion=12.10 \
    -Dpackaging=jar

# 4. Build the project
mvn clean package -q
```

---

## Usage

### Running a Single Instance

Generate all non-isomorphic connected d-regular graphs on n vertices:

```bash
java -jar target/optrevlex-graph-gen.jar \
    --n 12 \
    --d 3 \
    --config OptRevLex_D
```

**Options:**

| Flag | Description | Values |
|---|---|---|
| `--n` | Number of vertices | Integer ≥ 4 |
| `--d` | Degree of regularity | Integer, `d < n/2` recommended |
| `--config` | Solver configuration | `OptLex`, `OptRevLex`, `OptLex_P`, `OptRevLex_P`, `OptRevLex_D` |
| `--all` | Enumerate all graphs (not just connected) | Flag (no value) |
| `--timeout` | Time limit in seconds | Integer (default: 3600) |
| `--verbose` | Print each solution found | Flag (no value) |

**Example — enumerate all connected 3-regular graphs on 14 vertices:**

```bash
java -jar target/optrevlex-graph-gen.jar --n 14 --d 3 --config OptRevLex_D
# Expected: 18,025 solutions in ~16.5 s
```

**Example — compare OptLex vs OptRevLex on K₁₆(2):**

```bash
java -jar target/optrevlex-graph-gen.jar --n 16 --d 2 --config OptLex
# Expected: 1,246 solutions (all graphs) in ~4.05 s

java -jar target/optrevlex-graph-gen.jar --n 16 --d 2 --config OptRevLex
# Expected:    88 solutions (all graphs) in ~1.65 s  (14.2× fewer redundant solutions)
```

---

### Reproducing the Full Benchmark

To reproduce all results from Table 1 of the paper (six degree families, five configurations):

```bash
chmod +x scripts/run_full_benchmark.sh
./scripts/run_full_benchmark.sh
```

Results are written to `results/benchmark_results.csv` with columns:  
`instance, config, solutions, cpu_time_s`

> **Hardware note:** The paper's results were obtained on an HP ProDesk 400 G4 MT (Intel Core i5-7500 @ 3.40 GHz, 4 cores, 23 GB RAM) running Ubuntu 22.04.4 LTS. Absolute runtimes will vary by machine; relative rankings between configurations are robust.

---

## Configurations

The framework implements five configurations corresponding exactly to the paper's Table 1:

| Configuration | Symmetry Breaking | Connectivity | Output |
|---|---|---|---|
| **OptLex** | Optimized Lexicographic (Codish et al., 2018) | None | All graphs (connected + disconnected) |
| **OptRevLex** | Optimized Reverse Lexicographic *(this work)* | None | All graphs (connected + disconnected) |
| **OptLex(P)** | OptLex | Path-based encoding | Connected graphs only |
| **OptRevLex(P)** | OptRevLex | Path-based encoding | Connected graphs only |
| **OptRevLex(D)** | OptRevLex | Upper off-diagonal *(this work)* | Connected graphs only |

**Recommendation:** Use **OptRevLex(D)** for connected regular graph enumeration with `d < n/2`. For `d ≥ n/2`, apply complementation: run OptRevLex on K_n(n−1−d) and use the complementation identity.

---

## Benchmark Results (Summary)

All results from the paper (Table 1) at a glance. Times in seconds; `≪0.1` means below the JVM noise threshold.

### Speedup of OptRevLex(D) over OptLex(P) — Connected Graphs

| Instance | Solutions | OptLex(P) | OptRevLex(D) | Speedup |
|---|---|---|---|---|
| K₁₄(2) | 1 | 0.70 s | 0.17 s | **4.1×** |
| K₁₆(2) | 1 | 6.12 s | 0.61 s | **10.0×** |
| K₁₈(2) | 1 | 25.21 s | 7.72 s | **3.3×** |
| K₂₀(2) | 1 | 434.72 s | 21.48 s | **20.2×** |
| K₁₂(3) | 1,429 | 2.26 s | 0.62 s | **3.6×** |
| K₁₄(3) | 18,025 | 152.01 s | 16.52 s | **9.2×** |
| K₁₂(4) | 45,722 | 40.22 s | 8.46 s | **4.8×** |
| K₁₃(4) | 436,881 | 705.71 s | 129.86 s | **5.4×** |

### Symmetry Breaking Quality — Solution Count Reduction (All Graphs)

| Instance | OptLex | OptRevLex | Reduction |
|---|---|---|---|
| K₁₆(2) | 1,246 | 88 | **14.2×** |
| K₁₈(2) | 6,892 | 189 | **36.5×** |
| K₂₀(2) | 42,696 | 406 | **105.2×** (99% eliminated) |
| K₁₄(3) | 172,753 | 18,345 | **9.4×** |
| K₁₃(4) | 1,481,783 | 436,913 | **3.4×** |

> All timings from: HP ProDesk 400 G4 MT · Intel Core i5-7500 @ 3.40 GHz · Ubuntu 22.04.4 LTS · IBM ILOG CP Optimizer v12.10 · JVM (Java).

---

## How It Works

### OptRevLex Symmetry Breaking

The adjacency matrix rows are constrained to be in **Optimized Reverse Lexicographic (OptRevLex)** order. Row `i` precedes row `j` (i < j) under OptRevLex if: whenever `a[i][k] > a[j][k]` for some column k, there exists l > k such that `a[i][l] < a[j][l]`.

**Key theorem (Theorem 1):** Every canonical adjacency matrix (the one maximizing the row-concatenation string among all isomorphic matrices) satisfies OptRevLex order. This allows limiting the search to OptRevLex-ordered matrices — a necessary condition for canonicity — using O(n²) constraints of arity 2n, instead of the intractable O(n!) lex-leader approach.

OptRevLex pushes **ones toward the left** in early rows, which is the directional opposite of OptLex (which pushes ones toward the right). This directional property is what enables the connectivity encoding below.

### Connectivity Encodings

**Path-Based Encoding (OptLex(P) / OptRevLex(P)):**  
Introduces n auxiliary variables z₁,...,zₙ where zᵢ encodes the BFS distance from vertex i to a fixed root vertex 1. Constraints enforce that each vertex has a neighbor at distance one less. A tightening constraint `|zᵢ − zⱼ| ≤ 1` for all adjacent pairs ensures each connected adjacency matrix has exactly one consistent z-assignment (Theorem 2), preventing redundant enumeration. Works with any row ordering, but adds overhead.

**Upper Off-Diagonal Encoding (OptRevLex(D)):**  
Enforces the n−1 constraints:

```
∑_{j=i+1}^{n} a[i][j] ≥ 1,    for i = 1, ..., n−1
```

This requires every row i to have at least one neighbor with a higher index. Under OptRevLex ordering, this is both **necessary and sufficient** for connectivity of regular graphs (Theorem 3). No auxiliary variables are introduced.

### The Synergy

The upper off-diagonal encoding is **structurally incompatible with OptLex**. Under OptLex, ones are pushed right, so valid connected graphs can have rows that violate the off-diagonal constraint — making it unsound to prune them. Under OptRevLex, ones are pushed left, so any row lacking a higher-indexed neighbor genuinely signals disconnectivity.

This means OptRevLex(D) achieves two compounding gains simultaneously:
- OptRevLex generates far fewer redundant candidates than OptLex (symmetry gain).
- The off-diagonal encoding checks connectivity in O(n) with zero variable overhead (propagation gain).

Neither gain is available without the other in this combination.

---

## Scope and Limitations

- **Proven scope:** The completeness of the off-diagonal encoding (Theorem 3) is proved for **d-regular graphs** and relies jointly on regularity and the OptRevLex structural property.
- **Dense graphs (d ≥ n/2):** Handled via the complementation identity — run OptRevLex on K_n(n−1−d) instead.
- **Non-regular families:** The theoretical guarantees do not directly extend to non-regular graphs. This is an open research direction.
- **Solver dependency:** The implementation uses IBM ILOG CP Optimizer (commercial). Porting to open-source solvers (OR-Tools, Choco, MiniZinc) is a planned extension.

---

## Citation

If you use this code or build on this work, please cite:

```bibtex
@article{OmraniNaanaa2025optrevlex,
  author    = {Omrani, Mohamed Amine and Naanaa, Wady},
  title     = {Reverse Lexicographic Ordering and Upper Off-Diagonal
               Connectivity Constraints: A Synergistic Approach for
               Connected Non-Isomorphic Graph Generation},
  journal   = { ? ? ? },
  publisher = {? ? ?},
  year      = {2025},
  note      = {Under review}
}
```

This work builds on:

```bibtex
@article{Codish2018,
  author  = {Codish, Michael and Miller, Alice and Prosser, Patrick and Stuckey, Peter J.},
  title   = {Constraints for symmetry breaking in graph representation},
  journal = {Constraints},
  volume  = {24},
  number  = {1},
  pages   = {1--24},
  year    = {2018},
  doi     = {10.1007/s10601-018-9294-5}
}

@article{McKayPiperno2014,
  author  = {McKay, Brendan D. and Piperno, Adolfo},
  title   = {Practical graph isomorphism, {II}},
  journal = {Journal of Symbolic Computation},
  volume  = {60},
  pages   = {94--112},
  year    = {2014},
  doi     = {10.1016/j.jsc.2013.09.003}
}
```

---

## Authors

| Name | Affiliation | Email |
|---|---|---|
| **Mohamed Amine Omrani** | Private Higher Polytechnic School of Monastir, University of Monastir; LIMTIC Laboratory, ISI, University of Tunis El Manar | mohamedamine.omrani@fst.utm.tn |
| **Wady Naanaa** | National Engineering School of Tunis (ENIT); L3S Signals and Smart Systems Laboratory, University of Tunis El Manar | wady.naanaa@enit.utm.tn |

Both authors contributed equally to this work.

---

## License

This project is released under the [MIT License](LICENSE).  
The IBM ILOG CP Optimizer solver is proprietary software subject to its own license terms.

---

*For questions, issues, or collaboration inquiries, please open a GitHub issue or contact the authors directly.*
