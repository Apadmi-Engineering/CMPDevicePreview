# Contributing

## Commit messages

Releases are automated. [release-please](https://github.com/googleapis/release-please) reads the commit messages that land on `main` to build the changelog and choose the next version, so each one follows [Conventional Commits](https://www.conventionalcommits.org): `type(scope): description`.

| Type | Use it for | In the changelog |
|---|---|---|
| `feat` | a new capability | yes |
| `fix` | a bug fix | yes |
| `perf`, `revert` | a performance improvement, a revert | yes |
| `docs`, `refactor`, `build`, `ci`, `test`, `style`, `chore` | everything else | no, and on their own they don't trigger a release |

Mark a breaking change with `!` after the type, as in `refactor!: describe devices in dp`, or with a `BREAKING CHANGE:` footer. Breaking changes are always in the changelog.

Install the commit-message check once per clone. It rejects a message that isn't in this format:

```bash
git config core.hooksPath .githooks
```

When a pull request is squash-merged, its title becomes the commit message on `main`, so write the title in the same format. The hook can't check it.

## Releases

The version is in `version.txt`. Don't edit it by hand: release-please owns it and the changelog.

1. release-please keeps a pull request open on `main` titled `chore(main): release <version>`, listing what has changed since the last release.
2. Merging that pull request tags the release and creates the GitHub release. The tag starts the **Deploy** workflow, which publishes the library to Maven Central.
3. Until 1.0, a breaking change bumps the minor version (`0.0.1` to `0.1.0`) and a `feat` or `fix` bumps the patch (`0.1.0` to `0.1.1`). Releases are marked as pre-releases on GitHub. To force a particular version, such as `1.0.0`, add a `Release-As: <version>` footer to a commit message.

If a publish fails after the release was created, re-run the failed **Deploy** run from the Actions tab.

## Dependency updates

Renovate runs on the first of each month and opens its pull requests in at most three groups: Compose, Kotlin, and all other non-breaking updates. A major update gets its own pull request. Run the **Renovate** workflow from the Actions tab to get updates sooner, or to have it act on a rebase or Dependency Dashboard request.
