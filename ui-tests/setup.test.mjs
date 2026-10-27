import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

async function setupAndroid(overrides = {}) {
  const directory = await mkdtemp(join(tmpdir(), "ferrostar-android-setup-"));
  try {
    const bin = join(directory, "bin");
    const log = join(directory, "adb.log");
    await mkdir(bin);
    await writeFile(
      join(bin, "adb"),
      `#!/bin/sh
printf '%s\\n' "$*" >> "$ADB_LOG"
case "$*" in
  'shell getprop ro.kernel.qemu') printf '%s' "$EMULATOR" ;;
  'shell getprop persist.sys.locale') printf '%s' "$PERSISTED_LOCALE" ;;
  'shell getprop ro.product.locale') printf '%s' "$BOOT_LOCALE" ;;
  'shell su 0 setprop persist.sys.locale '*) exit "$SET_LOCALE_EXIT" ;;
  'reverse tcp:8765 tcp:8765') exit 0 ;;
  *) exit 99 ;;
esac
`,
      { mode: 0o755 },
    );
    // Stop at server setup: these tests must not launch a server or contact a real device.
    for (const [name, status] of [
      ["node", 1],
      ["curl", 1],
      ["sleep", 0],
    ]) {
      await writeFile(join(bin, name), `#!/bin/sh\nexit ${status}\n`, {
        mode: 0o755,
      });
    }
    const result = spawnSync(
      "bash",
      [fileURLToPath(new URL("./setup.sh", import.meta.url)), "android"],
      {
        encoding: "utf8",
        timeout: 5000,
        env: {
          ...process.env,
          PATH: `${bin}:${process.env.PATH}`,
          RUNNER_TEMP: directory,
          ADB_LOG: log,
          EMULATOR: "1",
          PERSISTED_LOCALE: "",
          BOOT_LOCALE: "en-US",
          SET_LOCALE_EXIT: "0",
          ...overrides,
        },
      },
    );
    assert.ifError(result.error);
    return {
      ...result,
      commands: (await readFile(log, "utf8")).trim().split("\n"),
    };
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
}

test("fresh emulator persists its boot locale before setting up routing", async () => {
  const result = await setupAndroid();
  assert.equal(result.status, 1); // The deliberately disabled fixture server.
  assert.deepEqual(result.commands, [
    "shell getprop ro.kernel.qemu",
    "shell getprop persist.sys.locale",
    "shell getprop ro.product.locale",
    "shell su 0 setprop persist.sys.locale en-US",
    "reverse tcp:8765 tcp:8765",
  ]);
});

test("does not overwrite an existing locale preference", async () => {
  const result = await setupAndroid({ PERSISTED_LOCALE: "ar-EG" });
  assert(!result.commands.some((command) => command.includes("setprop")));
  assert.equal(result.commands.at(-1), "reverse tcp:8765 tcp:8765");
});

test("never changes locale properties on a physical device", async () => {
  const result = await setupAndroid({ EMULATOR: "0" });
  assert.deepEqual(result.commands, [
    "shell getprop ro.kernel.qemu",
    "reverse tcp:8765 tcp:8765",
  ]);
});

test("missing boot locale fails instead of guessing a language", async () => {
  const result = await setupAndroid({ BOOT_LOCALE: "" });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /does not declare a boot locale/);
  assert(
    !result.commands.some(
      (command) => command.includes("setprop") || command.startsWith("reverse"),
    ),
  );
});

test("a locale property write failure stops setup", async () => {
  const result = await setupAndroid({ SET_LOCALE_EXIT: "17" });
  assert.equal(result.status, 17);
  assert(!result.commands.some((command) => command.startsWith("reverse")));
});
