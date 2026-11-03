/**
 * Restore exported timeline pictures when Trailblaze's short JPEG timeout expires.
 * Its exporter already embeds recording frames keyed by capture ID, but the timeline
 * does not reference them when the recording is too large to embed as a video.
 * Only use a capture's own frame; never substitute a different action's screenshot.
 */
import { readFile, writeFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

export function completeWebReport(html) {
  let sessions = 0;
  const completed = html.replace(
    /(<script\b[^>]*\bid="tb-session-\d+"[^>]*>)([\s\S]*?)(<\/script>)/g,
    (_, open, json, close) => {
      const session = JSON.parse(json);
      sessions++;
      // A playable video already supplies the pictures; its still frames may not be embedded.
      if (session.videoClip || session.videoClips?.length)
        return open + json + close;
      for (const entry of session.trace) {
        if (entry.screenshotFile || !entry.captureIds?.length) continue;
        const filename = entry.captureIds
          .map((id) => `${id}.webp`)
          .find((name) => session.shots[name]?.startsWith("data:image/"));
        if (!filename) {
          throw new Error(`No embedded recording frame for capture ${entry.i}`);
        }
        entry.screenshotFile = filename;
        entry.shotEndTs = entry.ts;
      }
      // Keep arbitrary page text inert inside the HTML's JSON script element.
      return open + JSON.stringify(session).replaceAll("<", "\\u003c") + close;
    },
  );
  if (!sessions)
    throw new Error("No Trailblaze session payloads found in report");
  return completed;
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const filename = process.argv[2];
  if (!filename)
    throw new Error("Usage: complete-web-report.mjs <report.html>");
  const html = await readFile(filename, "utf8");
  await writeFile(filename, completeWebReport(html));
}
