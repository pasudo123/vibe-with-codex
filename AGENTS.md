# AGENTS.md

## Purpose
Single Source of Truth (SSoT) for this repository's Git/PR workflow.

## Workflow Policy (Strict)
Summary: All changes must go through a task branch and PR to `main`.

- Never work directly on `main`.
- Start every task on a new branch.
- Commit and push from the task branch.
- Open a PR targeting `main`.
- Merge via PR only.

## Branch Lifecycle
Summary: Follow one consistent branch lifecycle for every task.

1. Update local `main`.
2. Create and switch to a task branch (default: `feature/<topic>`).
3. Implement changes, then commit with clear message(s).
4. Push the task branch to remote.
5. Open PR to `main` and complete review/checks.
6. Merge PR.
7. Delete the merged task branch in both local and remote.
8. Switch back to `main` and sync latest state.

## PR Best Practices
Summary: Keep PRs focused, verifiable, and easy to review.

- Title: concise and action-oriented.
- Scope: one intent per PR; avoid mixed concerns.
- Description must include:
  - Why this change is needed.
  - What changed (key points only).
  - How it was tested (commands/results).
  - Risk and rollback note.
- Keep diff size reviewable; split large work if needed.
- Rebase/sync before merge to reduce integration risk.

## SSoT Governance
Summary: Avoid duplicated or conflicting operational rules.

- Git/PR operational policy must be defined only in this file.
- Do not duplicate the same rules in other repository docs.
- Other docs may link to `AGENTS.md` but must not restate policy details.
- If any document conflicts with this file, `AGENTS.md` is authoritative.
- Updates to policy must edit this file first.
