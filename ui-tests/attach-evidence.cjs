// Called directly for same-repository PRs, or by a default-branch workflow for fork PRs.
async function attachEvidence({ github, context, core, result }) {
  const repo = context.repo;
  const run =
    context.payload.workflow_run ??
    (
      await github.rest.actions.getWorkflowRun({
        ...repo,
        run_id: context.runId,
      })
    ).data;
  if (!run.head_repository?.id) {
    core.info(
      "Skipping evidence attachment: the source repository no longer exists.",
    );
    return;
  }
  const marker = `<!-- ferrostar-ui-evidence:${run.workflow_id} -->`;
  let candidates = run.pull_requests ?? [];
  // GitHub can omit pull_requests for workflows triggered from forks.
  if (!candidates.length) {
    candidates = await github.paginate(
      github.rest.repos.listPullRequestsAssociatedWithCommit,
      {
        ...repo,
        commit_sha: run.head_sha,
        per_page: 100,
      },
    );
  }

  const artifacts = await github.paginate(
    github.rest.actions.listWorkflowRunArtifacts,
    {
      ...repo,
      run_id: run.id,
      per_page: 100,
    },
  );
  const evidence = artifacts.filter(
    (artifact) =>
      !artifact.expired &&
      /^trailblaze-(?:report|sessions)-(?:android|ios|web)-[a-z0-9-]+(?:\.html)?$/.test(
        artifact.name,
      ),
  );
  const runUrl = `${context.serverUrl}/${repo.owner}/${repo.repo}/actions/runs/${run.id}`;
  const lines = [
    marker,
    `UI navigation evidence for ${run.head_sha.slice(0, 12)} — [workflow run](${runUrl}).`,
    "",
    "Recordings and step reports are for human review. Visual differences do not fail these checks; build, device, and flow errors do.",
    "",
    `Result: ${result ?? run.conclusion}.`,
    "",
    ...evidence.map(
      (artifact) => `- [${artifact.name}](${runUrl}/artifacts/${artifact.id})`,
    ),
  ];
  if (!evidence.length) {
    lines.push(
      "No UI evidence was produced. Check the workflow for build, setup, or cancellation errors.",
    );
  }
  lines.push(
    "",
    "Download the HTML report to open it locally, or open a session zip in the [Trailblaze viewer](https://block.github.io/trailblaze/report-viewer/). Artifacts expire according to the workflow retention policy.",
  );

  for (const candidate of candidates) {
    const { data: pr } = await github.rest.pulls.get({
      ...repo,
      pull_number: candidate.number,
    });
    // A completed older run must not replace evidence for a newer PR revision.
    if (
      pr.state !== "open" ||
      pr.head.sha !== run.head_sha ||
      pr.head.repo?.id !== run.head_repository?.id
    ) {
      core.info(
        `Skipping PR #${pr.number}: closed, different repository, or superseded revision.`,
      );
      continue;
    }
    const comments = await github.paginate(github.rest.issues.listComments, {
      ...repo,
      issue_number: pr.number,
      per_page: 100,
    });
    const previous = comments.find(
      (comment) =>
        comment.user?.login === "github-actions[bot]" &&
        comment.body?.startsWith(marker),
    );
    if (previous) {
      await github.rest.issues.updateComment({
        ...repo,
        comment_id: previous.id,
        body: lines.join("\n"),
      });
    } else {
      await github.rest.issues.createComment({
        ...repo,
        issue_number: pr.number,
        body: lines.join("\n"),
      });
    }
  }
}

module.exports = attachEvidence;
