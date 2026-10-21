# Contributing to FuelFinder

Thank you for contributing to **FuelFinder**! This document defines the workflow and collaboration guidelines for the project.

> **Important:** The repository is **private**, owned by a **personal account**, and uses **GitHub Free**. In this setup, Pull Request approvals and `main` branch protection **are not automatically enforced by GitHub**. Adherence to the rules below is the responsibility of the entire team.

## 1. Roles and Responsibilities

Approval by another developer **does not substitute** Tech Lead approval. **No one approves their own PR.**

## 2. `main` Branch Rules

1. `main` represents the integrated and reviewed version of the project.
2. **Do not push directly to `main`.**
3. All changes must be submitted via **Pull Request (PR)**.
4. Developer PRs can only be merged **after explicit Tech Lead approval**.
5. The **Tech Lead is responsible for merging**. If the PR is authored by the Tech Lead, the **owner must review and approve it prior to merging**.
6. If changes are made after approval, a **new review must be requested**.
7. Pending review comments must be resolved before merging.
8. Do not force push to `main` or delete the branch.

> These rules are **team agreements**: under the current plan, GitHub will not technically block all violations.

## 3. Branching Conventions

Create a branch off an up-to-date `main`. Use descriptive, lowercase names with hyphens.

| Prefix | When to Use | Example |
| --- | --- | --- |
| `feat/` | New feature | `feat/nearby-stations-search` |
| `fix/` | Bug fix | `fix/liter-calculation` |
| `refactor/` | Refactoring without behavior change | `refactor/pricing-service` |
| `test/` | Adding or improving tests | `test/station-review` |
| `docs/` | Documentation | `docs/pr-rules` |
| `chore/` | Configuration and maintenance | `chore/docker-compose` |

## 4. Contribution Workflow

1. Update your local copy of `main`.
2. Create a branch for the change.
3. Implement the change respecting the project's architectural decisions.
4. Run relevant tests and perform a self-code review.
5. Push the branch to GitHub and open a PR targeting `main`.
6. Fill out the PR template file: description, changes, validation, and checklist.
7. Add the **Tech Lead as a reviewer** on GitHub. For PRs by the Tech Lead, request a review from the **owner**.
8. Address feedback raised during review and request a new review when necessary.
9. Await explicit approval and discussion resolution; the **Tech Lead performs a Squash and Merge** after approval. For PRs by the Tech Lead, approval must come from the owner.
10. After the merge, the working branch can be deleted.

### Command Example

```bash
git switch main
git pull origin main
git switch -c feat/my-feature

# After implementing and validating:
git add .
git commit -m "feat: add my feature"
git push -u origin feat/my-feature