import mapLibreGL from "maplibre-gl";
// Side-effect import: registers the <ferrostar-map> custom element.
import "@stadiamaps/ferrostar-webcomponents";
import { ReplayController } from "./replay-controller";
import { extractImportantEvents, ImportantEvent } from "./replay-events";

declare global {
  interface Window {
    lucide?: { createIcons: () => void };
  }
}

const formatTime = (ms: number): string => {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms / 1000) % 60)
    .toString()
    .padStart(2, "0");
  return `${minutes}:${seconds}`;
};

const makeEventRow = (evt: ImportantEvent): HTMLDivElement => {
  const row = document.createElement("div");
  row.className = "event-item";

  const time = document.createElement("span");
  time.className = "event-time";
  time.textContent = formatTime(evt.offsetMs);

  const tag = document.createElement("span");
  tag.className = `event-tag ${evt.tagClass}`;
  tag.textContent = evt.type;

  const label = document.createElement("span");
  label.className = "event-label";
  label.textContent = evt.label;

  row.append(time, tag, label);
  return row;
};

const main = async () => {
  const mapInstance = new mapLibreGL.Map({
    container: "mapElement",
    style: "https://tiles.stadiamaps.com/styles/outdoors.json",
    center: [0, 0],
    zoom: 0.5,
    attributionControl: { compact: true },
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const ferrostarMap = document.getElementById("ferrostar") as any;
  ferrostarMap.map = mapInstance;
  ferrostarMap.geolocateOnLoad = false;
  ferrostarMap.customStyles = `
    instructions-view,
    #bottom-component,
    #stop-button {
      pointer-events: auto;
    }
  `;

  const replayButton = document.getElementById("replay") as HTMLButtonElement;
  const replayFile = document.getElementById("replayFile") as HTMLInputElement;
  const controls = document.getElementById("replayControls") as HTMLElement;
  const playBtn = document.getElementById("playPauseBtn") as HTMLButtonElement;
  const stopBtn = document.getElementById("stopBtn") as HTMLButtonElement;
  const slider = document.getElementById("progressSlider") as HTMLInputElement;
  const speedSelect = document.getElementById(
    "speedSelect",
  ) as HTMLSelectElement;
  const timeDisplay = document.getElementById("timeDisplay") as HTMLElement;
  const eventsSection = document.getElementById("eventsSection") as HTMLElement;
  const eventsList = document.getElementById("eventsList") as HTMLElement;

  let currentReplay: ReplayController | null = null;
  let progressInterval: ReturnType<typeof setInterval> | null = null;
  let events: ImportantEvent[] = [];
  let currentEventIdx = -1;

  const clearProgressInterval = () => {
    if (progressInterval === null) return;
    clearInterval(progressInterval);
    progressInterval = null;
  };

  const renderEvents = () => {
    if (!events.length) {
      eventsList.replaceChildren();
      eventsSection.style.display = "none";
      currentEventIdx = -1;
      return;
    }
    eventsSection.style.display = "block";
    eventsList.replaceChildren(...events.map(makeEventRow));
    currentEventIdx = -1;
  };

  const updateCurrentEvent = (elapsedMs: number) => {
    if (!events.length) return;
    const idx = events.findLastIndex((e) => e.offsetMs <= elapsedMs);
    if (idx === currentEventIdx) return;
    eventsList.children[currentEventIdx]?.classList.remove("event-current");
    const next = eventsList.children[idx];
    if (next) {
      next.classList.add("event-current");
      next.scrollIntoView({ block: "nearest", behavior: "smooth" });
    }
    currentEventIdx = idx;
  };

  const startProgressInterval = () => {
    clearProgressInterval();
    progressInterval = setInterval(() => {
      if (!currentReplay?.playing) {
        clearProgressInterval();
        return;
      }
      slider.value = String(currentReplay.progress);
      const current = (currentReplay.progress / 100) * currentReplay.duration;
      timeDisplay.textContent = `${formatTime(current)} / ${formatTime(
        currentReplay.duration,
      )}`;
      updateCurrentEvent(current);
    }, 100);
  };

  const setPlayPauseIcon = (name: "play" | "pause") => {
    const icon = document.createElement("i");
    icon.dataset.lucide = name;
    playBtn.replaceChildren(icon);
    window.lucide?.createIcons();
  };

  replayButton.addEventListener("click", () => replayFile.click());

  replayFile.addEventListener("change", async () => {
    const file = replayFile.files?.[0];
    if (!file) return;

    clearProgressInterval();

    const json = await file.text();
    try {
      currentReplay = new ReplayController(json);
    } catch (err) {
      console.error("Failed to parse recording JSON:", err);
      replayFile.value = "";
      return;
    }

    events = extractImportantEvents(currentReplay.events);
    renderEvents();

    const start = currentReplay.initialRoute?.geometry?.[0];
    if (start) {
      mapInstance.jumpTo({
        center: [start.lng, start.lat],
        zoom: 16,
        pitch: 45,
      });
    }

    ferrostarMap.linkWith(currentReplay, true);
    ferrostarMap.showNavigationUI = false;

    slider.value = "0";
    speedSelect.value = "1";
    setPlayPauseIcon("pause");
    controls.style.display = "flex";

    currentReplay.onNavigationStart = () => {
      replayButton.disabled = true;
    };

    currentReplay.onNavigationStop = () => {
      replayButton.disabled = false;
      controls.style.display = "none";
      clearProgressInterval();
    };

    startProgressInterval();

    await currentReplay.play();
    replayFile.value = "";
  });

  playBtn.addEventListener("click", () => {
    if (!currentReplay) return;
    if (currentReplay.playing) {
      currentReplay.pause();
      setPlayPauseIcon("play");
    } else {
      currentReplay.play();
      setPlayPauseIcon("pause");
      startProgressInterval();
    }
  });

  stopBtn.addEventListener("click", () => {
    ferrostarMap.stopNavigation();
    clearProgressInterval();
  });

  speedSelect.addEventListener("change", (e) => {
    const speed = parseFloat((e.target as HTMLSelectElement).value);
    currentReplay?.setPlaybackSpeed(speed);
  });

  slider.addEventListener("input", (e) => {
    if (!currentReplay) return;
    const value = parseFloat((e.target as HTMLInputElement).value);
    const { duration } = currentReplay;
    const current = (value / 100) * duration;
    timeDisplay.textContent = `${formatTime(current)} / ${formatTime(duration)}`;
    currentReplay.seekToProgress(value);
    updateCurrentEvent(current);
  });

  window.lucide?.createIcons();
};

// Module scripts are deferred, so the DOM is already parsed here.
main();
