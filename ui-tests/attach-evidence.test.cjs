const assert = require("node:assert/strict");
const { test } = require("node:test");
const attachEvidence = require("./attach-evidence.cjs");

function fixture({
  candidates = [{ number: 7 }],
  comments = [],
  artifacts,
  pr = {},
} = {}) {
  const writes = [];
  const repository = { owner: "stadiamaps", repo: "ferrostar" };
  const run = {
    id: 99,
    workflow_id: 12,
    head_sha: "abcdef1234567890",
    head_repository: { id: 42 },
    conclusion: "success",
    pull_requests: candidates,
  };
  const rest = {
    repos: { listPullRequestsAssociatedWithCommit: "associated" },
    actions: {
      listWorkflowRunArtifacts: "artifacts",
      getWorkflowRun: async (args) => {
        assert.deepEqual(args, { ...repository, run_id: 99 });
        return { data: run };
      },
    },
    pulls: {
      get: async () => ({
        data: {
          number: 7,
          state: "open",
          head: { sha: "abcdef1234567890", repo: { id: 42 } },
          ...pr,
        },
      }),
    },
    issues: {
      listComments: "comments",
      createComment: async (value) => writes.push({ type: "create", ...value }),
      updateComment: async (value) => writes.push({ type: "update", ...value }),
    },
  };
  return {
    writes,
    args: {
      github: {
        rest,
        paginate: async (method, args) => {
          assert.equal(args.owner, repository.owner);
          assert.equal(args.repo, repository.repo);
          if (method === "artifacts") {
            assert.equal(args.run_id, 99);
            return (
              artifacts ?? [
                {
                  id: 100,
                  name: "trailblaze-report-ios-iphone-17-pro-en-us.html",
                  expired: false,
                },
              ]
            );
          }
          if (method === "comments") return comments;
          if (method === "associated") return [{ number: 7 }];
          throw new Error(`Unexpected API call: ${method}`);
        },
      },
      context: {
        repo: repository,
        runId: 99,
        serverUrl: "https://github.com",
        payload: {
          workflow_run: run,
        },
      },
      core: { info: () => {} },
    },
  };
}

test("links evidence from the triggering run on the matching PR", async () => {
  const { args, writes } = fixture();
  await attachEvidence(args);
  assert.equal(writes.length, 1);
  assert.equal(writes[0].type, "create");
  assert.equal(writes[0].issue_number, 7);
  assert.match(writes[0].body, /actions\/runs\/99\/artifacts\/100/);
});

test("fork runs without a PR association resolve it from the commit", async () => {
  const { args, writes } = fixture({ candidates: [] });
  await attachEvidence(args);
  assert.equal(writes.length, 1);
});

for (const result of ["success", "failure", "cancelled", "skipped"]) {
  test(`direct PR publication reports the exercise result: ${result}`, async () => {
    const { args, writes } = fixture();
    // The enclosing workflow is still running when its reporting job starts.
    args.context.payload.workflow_run.conclusion = null;
    args.context.payload = {};
    await attachEvidence({ ...args, result });
    assert.equal(writes.length, 1);
    assert.match(writes[0].body, new RegExp(`Result: ${result}\\.`));
    assert.match(writes[0].body, /actions\/runs\/99\/artifacts\/100/);
  });
}

test("direct PR publication also rejects a superseded revision", async () => {
  const { args, writes } = fixture({
    pr: { head: { sha: "newer", repo: { id: 42 } } },
  });
  args.context.payload = {};
  await attachEvidence({ ...args, result: "success" });
  assert.deepEqual(writes, []);
});

test("a deleted source repository cannot attach evidence", async () => {
  const { args, writes } = fixture();
  args.context.payload.workflow_run.head_repository = null;
  await attachEvidence(args);
  assert.deepEqual(writes, []);
});

test("updates the existing bot comment without editing a user comment", async () => {
  const { args, writes } = fixture({
    comments: [
      {
        id: 1,
        user: { login: "human" },
        body: "<!-- ferrostar-ui-evidence:12 -->",
      },
      {
        id: 2,
        user: { login: "github-actions[bot]" },
        body: "<!-- ferrostar-ui-evidence:12 -->\nOld evidence",
      },
    ],
  });
  await attachEvidence(args);
  assert.equal(writes[0].type, "update");
  assert.equal(writes[0].comment_id, 2);
});

for (const [description, pr] of [
  ["closed PR", { state: "closed" }],
  ["superseded commit", { head: { sha: "newer", repo: { id: 42 } } }],
  ["different fork", { head: { sha: "abcdef1234567890", repo: { id: 43 } } }],
  ["deleted fork", { head: { sha: "abcdef1234567890", repo: null } }],
]) {
  test(`does not publish evidence for a ${description}`, async () => {
    const { args, writes } = fixture({ pr });
    await attachEvidence(args);
    assert.deepEqual(writes, []);
  });
}

test("filters expired artifacts and artifact names containing Markdown", async () => {
  const { args, writes } = fixture({
    artifacts: [
      { id: 1, name: "trailblaze-sessions-web-desktop-en-us", expired: false },
      { id: 2, name: "trailblaze-report-ios-phone.html", expired: true },
      {
        id: 3,
        name: "trailblaze-report-web-[click](https://example.com)",
        expired: false,
      },
      { id: 4, name: "ui-exercise-android-app", expired: false },
    ],
  });
  await attachEvidence(args);
  assert.match(writes[0].body, /artifacts\/1\)/);
  assert.doesNotMatch(writes[0].body, /artifacts\/[234]\)|example\.com/);
});

test("reports missing evidence and preserves an infrastructure failure", async () => {
  const { args, writes } = fixture({ artifacts: [] });
  args.context.payload.workflow_run.conclusion = "failure";
  await attachEvidence(args);
  assert.match(writes[0].body, /Result: failure/);
  assert.match(writes[0].body, /No UI evidence was produced/);
});
