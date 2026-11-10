import assert from "node:assert/strict";
import { test } from "node:test";
import { completeWebReport } from "./complete-web-report.mjs";

const frame = "capture-123-abcdef.webp";
function payload(overrides = {}) {
  return {
    meta: { status: "failed" },
    video: null,
    shots: { [frame]: "data:image/webp;base64,AAAA" },
    trace: [
      {
        i: 1,
        ts: 123,
        screenshotFile: null,
        captureIds: ["capture-123-abcdef"],
        ok: false,
      },
      { i: 2, screenshotFile: null },
    ],
    ...overrides,
  };
}
function script(session, index = 0) {
  return `<script type="application/json" id="tb-session-${index}">${JSON.stringify(session).replaceAll("<", "\\u003c")}</script>`;
}
function sessions(html) {
  return [...html.matchAll(/<script[^>]*>(.*?)<\/script>/g)].map((match) =>
    JSON.parse(match[1]),
  );
}

test("references the exact embedded recording frame without changing the verdict", () => {
  const [session] = sessions(completeWebReport(script(payload())));
  assert.equal(session.trace[0].screenshotFile, frame);
  assert.equal(session.trace[0].shotEndTs, 123);
  assert.equal(session.trace[0].ok, false);
  assert.equal(session.meta.status, "failed");
  assert.equal(session.trace[1].screenshotFile, null);
});

test("preserves original screenshots and is idempotent", () => {
  const original = payload({
    trace: [{ i: 1, screenshotFile: "original.jpg", captureIds: ["other"] }],
  });
  const completed = completeWebReport(script(original));
  assert.deepEqual(sessions(completed), [original]);
  assert.equal(completeWebReport(completed), completed);
});

test("does not require fallback stills when a playable video is embedded", () => {
  const html = script(
    payload({
      shots: {},
      videoClip: { uri: "data:video/webm;base64,AAAA" },
    }),
  );
  assert.equal(completeWebReport(html), html);
});

test("repairs every session and preserves surrounding HTML", () => {
  const html = `<html>${script(payload())}${script(payload(), 1)}</html>`;
  const completed = completeWebReport(html);
  assert(completed.startsWith("<html>"));
  assert(completed.endsWith("</html>"));
  assert(
    sessions(completed).every(
      (session) => session.trace[0].screenshotFile === frame,
    ),
  );
});

test("never borrows another capture's picture or fetches a remote image", () => {
  for (const shots of [{}, { [frame]: "https://example.com/frame.webp" }]) {
    assert.throws(
      () => completeWebReport(script(payload({ shots }))),
      /No embedded recording frame/,
    );
  }
});

test("page text cannot close the JSON script element", () => {
  const session = payload({ title: "</script><script>alert(1)</script>" });
  const completed = completeWebReport(script(session));
  assert.equal((completed.match(/<script/g) ?? []).length, 1);
  assert.equal(sessions(completed)[0].title, session.title);
});

test("unexpected or malformed report formats fail explicitly", () => {
  assert.throws(
    () => completeWebReport("<html></html>"),
    /No Trailblaze session/,
  );
  assert.throws(
    () => completeWebReport('<script id="tb-session-0">invalid</script>'),
    SyntaxError,
  );
});
