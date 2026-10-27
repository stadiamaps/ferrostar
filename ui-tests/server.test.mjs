import assert from "node:assert/strict";
import { once } from "node:events";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { test } from "node:test";
import { createExerciseServer } from "./server.mjs";

test("serves routing fixtures and web assets without exposing the repository", async (t) => {
  const directory = await mkdtemp(join(tmpdir(), "ferrostar-ui-server-"));
  t.after(() => rm(directory, { recursive: true, force: true }));
  await writeFile(join(directory, "index.html"), "<title>Ferrostar</title>");
  await writeFile(
    join(directory, "core.wasm"),
    new Uint8Array([0, 97, 115, 109]),
  );
  const server = createExerciseServer(directory);
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;

  await t.test(
    "route response is the existing extended OSRM fixture",
    async () => {
      const response = await fetch(`${base}/route`, {
        method: "POST",
        body: "{}",
      });
      assert.equal(response.status, 200);
      assert.equal(response.headers.get("content-type"), "application/json");
      assert.deepEqual(
        await response.json(),
        JSON.parse(
          await readFile(
            new URL(
              "../common/ferrostar/src/fixtures/valhalla_extended_osrm_response.json",
              import.meta.url,
            ),
          ),
        ),
      );
    },
  );
  await t.test("demo query parameters do not affect asset lookup", async () => {
    const response = await fetch(`${base}/?uiExercise`);
    assert.equal(response.status, 200);
    assert.equal(await response.text(), "<title>Ferrostar</title>");
    assert.equal(response.headers.get("cache-control"), "no-store");
  });
  await t.test(
    "WASM has the MIME type needed for streaming compilation",
    async () => {
      const response = await fetch(`${base}/core.wasm`);
      assert.equal(response.headers.get("content-type"), "application/wasm");
      assert.deepEqual(
        new Uint8Array(await response.arrayBuffer()),
        new Uint8Array([0, 97, 115, 109]),
      );
    },
  );
  await t.test("HEAD omits the response body", async () => {
    const response = await fetch(`${base}/index.html`, { method: "HEAD" });
    assert.equal(response.status, 200);
    assert.equal(await response.text(), "");
  });
  await t.test(
    "health, missing files, malformed paths, and unsupported methods",
    async () => {
      assert.equal(
        await (await fetch(`${base}/health`)).text(),
        `ferrostar-ui:${process.pid}`,
      );
      assert.equal((await fetch(`${base}/missing.js`)).status, 404);
      assert.equal((await fetch(`${base}/%invalid`)).status, 400);
      assert.equal(
        (await fetch(`${base}/index.html`, { method: "POST" })).status,
        405,
      );
    },
  );
  await t.test("encoded traversal cannot escape the web build", async () => {
    assert.equal((await fetch(`${base}/..%2fpackage.json`)).status, 403);
  });
});
