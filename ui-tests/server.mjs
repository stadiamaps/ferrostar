/** Local routing fixture and built web demo, shared by all UI exercises. */
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, resolve, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));
const fixture = await readFile(
  new URL(
    "../common/ferrostar/src/fixtures/valhalla_extended_osrm_response.json",
    import.meta.url,
  ),
);
const contentTypes = {
  ".css": "text/css",
  ".html": "text/html",
  ".js": "text/javascript",
  ".json": "application/json",
  ".svg": "image/svg+xml",
  ".wasm": "application/wasm",
};

export function createExerciseServer(webRoot = resolve(root, "web/dist")) {
  const directory = resolve(webRoot);
  return createServer(async (request, response) => {
    response.setHeader("Cache-Control", "no-store");
    const url = new URL(request.url, "http://localhost");

    if (request.method === "GET" && url.pathname === "/health") {
      response
        .writeHead(200, { "Content-Type": "text/plain" })
        .end(`ferrostar-ui:${process.pid}`);
      return;
    }
    if (request.method === "POST" && url.pathname === "/route") {
      // Drain the request so persistent native HTTP connections can be reused.
      request.resume();
      response
        .writeHead(200, { "Content-Type": "application/json" })
        .end(fixture);
      return;
    }
    if (request.method !== "GET" && request.method !== "HEAD") {
      response.writeHead(405).end();
      return;
    }

    try {
      const pathname = decodeURIComponent(url.pathname);
      const path = resolve(
        directory,
        `.${pathname === "/" ? "/index.html" : pathname}`,
      );
      if (!path.startsWith(directory + sep)) {
        response.writeHead(403).end();
        return;
      }
      const body = await readFile(path);
      response.writeHead(200, {
        "Content-Type":
          contentTypes[extname(path)] ?? "application/octet-stream",
      });
      response.end(request.method === "HEAD" ? undefined : body);
    } catch (error) {
      const status =
        error instanceof URIError ? 400 : error.code === "ENOENT" ? 404 : 500;
      if (status === 500) console.error(error);
      response.writeHead(status).end();
    }
  });
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(resolve(process.argv[1])).href
) {
  const server = createExerciseServer();
  server.on("error", (error) => {
    console.error(error);
    process.exitCode = 1;
  });
  server.listen(8765, "127.0.0.1", () => {
    console.log("UI exercise server listening on http://localhost:8765");
  });
}
