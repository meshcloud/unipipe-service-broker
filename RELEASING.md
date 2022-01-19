# Release Process

> There are certain parts about the process that could be automated better, pull requests are welcome.

1. Pick a new version number according to [semver](https://semver.org/).
   Note that we consider the configuration parameters part of unipipe service broker's "API".
   Note that we consider the commands and output part of unipipe cli's "API".
  
2. Update the version number in these places:
    - `cli/unipipe/info.ts`
    - `examples/pipeline/github-actions/vnet.yml`
    - `cli/unipipe/blueprints/github-workflow.yml.js`

3. Update `CHANGELOG.md`. We follow the guidelines from
   [Keep a Changelog][keep-a-changelog]. It's recommended to add changes for the next version in a vNext section already together with pull requests, as this will make releasing a lot easier.

4. Commit changes. Use the convention "Release vX.X.X” in your commit message.
   There shouldn’t be any other code changes.

5. Tag the commit with the version number `git tag vX.X.X`

6. Push the release tag `git push origin --tags`

7. CI will run the [release workflow](.github/workflows/release-workflow.yml) and create a new [GitHub Release](github-release).

8. Manually edit the release on GitHub and paste the entries from `CHANGELOG.md` into the release description.

[keep-a-changelog]: https://keepachangelog.com/en/1.0.0/
[github-release]: https://github.com/meshcloud/unipipe-service-broker/releases/new
