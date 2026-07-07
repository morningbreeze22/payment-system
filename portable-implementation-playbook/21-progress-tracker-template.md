> **Purpose:** Blank progress tracker mirroring the linear order in 20-execution-sequence-and-decision-defaults.md — the executor's durable memory across sessions.
> **When to use this file:** Copy to a LOCAL untracked file at D-01 time; update after EVERY task (status + report filed); read at the start of EVERY session to find the next row.
> **Depends on:** 20-execution-sequence-and-decision-defaults.md.
> **Used by:** The local coding agent (session start/end); the human driver (review).
> **Safe to transfer:** yes (BLANK template only — once filled, the Notes column may contain local names; the filled copy must NEVER leave the work laptop or be sent externally)
> **Contains local code names:** no while blank; possibly YES after local fill — the filled copy stays on the work laptop, never transferred

# Progress tracker (local copy — fill on the work laptop only)

Status values: TODO / IN-PROGRESS / DONE / BLOCKED(reason) / WAITING-HUMAN.
Session-start rule: read this table top to bottom; resume at the first
row that is IN-PROGRESS, else take the first TODO whose Wait-on is
satisfied (per file 20). Session-end rule: no row may be left without
a status; every DONE row has a filed execution report (file 19).

| # | Task | Status | Date | Report filed? | Notes (LOCAL ONLY) |
|---|------|--------|------|---------------|--------------------|
| 1 | D-01 | TODO | | | |
| 2 | D-02 | TODO | | | |
| 3 | D-03 | TODO | | | |
| 4 | D-04 | TODO | | | |
| 5 | D-05 | TODO | | | |
| 6 | D-06 | TODO | | | |
| 7 | D-07 | TODO | | | |
| 8 | D-08 | TODO | | | |
| 9 | D-09 | TODO | | | |
| 10 | D-10 | TODO | | | |
| 11 | D-11 | TODO | | | |
| 12 | D-12 | TODO | | | |
| — | HUMAN REVIEW of D-12 | TODO | | | |
| 13 | B-01 | TODO | | | |
| 14 | B-02 | TODO | | | |
| 15 | B-03 | TODO | | | |
| 16 | B-04 | TODO | | | |
| 17 | CA-1 | TODO | | | |
| 18 | CA-2 | TODO | | | |
| 19 | CA-3 | TODO | | | |
| 20 | CA-4 | TODO | | | |
| 21 | CA-5 | TODO | | | |
| 22 | CA-6 | TODO | | | |
| 23 | CA-7 | TODO | | | |
| 24 | CA-8 | TODO | | | |
| 25 | CA-9 | TODO | | | |
| 26 | S-01 | TODO | | | |
| 27 | S-02 | TODO | | | |
| 28 | S-03 | TODO | | | |
| 29 | S-04 | TODO | | | |
| 30 | S-05 | TODO | | | |
| 31 | S-06 | TODO | | | |
| 32 | S-07 | TODO | | | |
| 33 | S-08 | TODO | | | |
| 34 | S-09 | TODO | | | |
| 35 | K-01 | TODO | | | |
| 36 | K-02 | TODO | | | |
| 37 | K-03 | TODO | | | |
| 38 | K-04 | TODO | | | |
| 39 | K-05 | TODO | | | |
| 40 | K-06 | TODO | | | |
| 41 | CT-01 | TODO | | | (DD-6 if no sandbox yet) |
| 42 | CT-02 | TODO | | | |
| 43 | CT-03 | TODO | | | |
| 44 | CT-04 | TODO | | | |
| 45 | CT-05 | TODO | | | |
| 46 | CT-06 | TODO | | | |
| 47 | CT-07 | TODO | | | |
| 48 | U-01 | TODO | | | |
| 49 | U-02 | TODO | | | |
| 50 | U-03 | TODO | | | (DD-5 pending case) |
| 51 | ST-01 | TODO | | | |
| 52 | ST-02 | TODO | | | |
| 53 | ST-03 | TODO | | | (DD-7 pending case) |
| 54 | ST-04 | TODO | | | |
| 55 | ST-05 | TODO | | | (split per rule site) |
| 56 | ST-06 | TODO | | | (DD-7 pending case) |
| 57 | ST-07 | TODO | | | |
| 58 | ST-08 | TODO | | | |
| 59 | ST-09 | TODO | | | (DD-1) |
| 60 | ST-10 | TODO | | | |
| 61 | ST-11 | TODO | | | |
| 62 | RG-01 | TODO | | | |
| 63 | RG-02 | TODO | | | |
| 64 | RG-03 | TODO | | | |
| 65 | RG-04 | TODO | | | |
| 66 | RG-05 | TODO | | | |
| 67 | RG-06 | TODO | | | (DD-3: T1 pending IN-02) |
| 68 | RG-07 | TODO | | | (DD-7 pending case) |
| 69 | RG-08 | TODO | | | (DD-2: shared liveness helper) |
| 70 | RG-09 | TODO | | | |
| 71 | RG-10 | TODO | | | |
| 72 | IN-01 | TODO | | | |
| 73 | IN-02 | TODO | | | (DD-3: wire T1 here) |
| 74 | IN-03 | TODO | | | |
| 75 | IN-04 | TODO | | | (DD-2: extend the RG-08 helper) |
| 76 | IN-05 | TODO | | | |
| 77 | IN-06 | TODO | | | (close DD-5 here) |
| 78 | IN-07 | TODO | | | |
| 79 | IN-08 | TODO | | | |
| 80 | IN-09 | TODO | | | |
| 81 | RC-01 | TODO | | | |
| 82 | RC-02 | TODO | | | |
| 83 | RC-03 | TODO | | | (DD-4 stubs) |
| 84 | RC-04 | TODO | | | |
| 85 | RC-05 | TODO | | | |
| 86 | RC-06 | TODO | | | |
| 87 | RC-07 | TODO | | | (then close DD-7 cases) |
| 88 | RC-08 | TODO | | | (then close ST-06 case) |
| 89 | RC-09 | TODO | | | (replace DD-4 freeze stub) |
| 90 | RC-10 | TODO | | | |
| — | DD-7 pending-case closure sweep | TODO | | | |
| 91 | OP-01 | TODO | | | |
| 92 | OP-02 | TODO | | | |
| 93 | OP-03 | TODO | | | (HUMAN drill) |
| 94 | OB-01 | TODO | | | |
| 95 | OB-02 | TODO | | | |
| 96 | OB-03 | TODO | | | |
| 97 | OB-04 | TODO | | | |
| 98 | OB-05 | TODO | | | |
| 99 | OB-06 | TODO | | | |
| 100 | OB-07 | TODO | | | |
| 101 | GO-01 | TODO | | | |
| 102 | GO-02 | TODO | | | |
| 103 | GO-03 | TODO | | | (F4 needs CT PASS — DD-6) |
| 104 | GO-04 | TODO | | | (HUMAN go/no-go) |
| 105 | GO-05 | TODO | | | |
