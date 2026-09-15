### Build composable components
	
Split large controls and feature components into small components with clear responsibilities. When several siblings need the same feature state, put that state in a nearby provider instead of keeping the entire feature in one component.
	
```tsx
// Avoid: one component owns the state and every part of the feature UI.
function RoutePlanner() {
  const [route, setRoute] = useState<Route>();
  return <div>{/* header, editor, listing, validation, actions... */}</div>;
}

// Prefer: a provider coordinates focused, reusable pieces.
function RoutePlanner() {
  return (
    <RoutePlannerProvider>
      <RoutePlannerHeader />
      <RouteEditor />
      <RouteListing />
    </RoutePlannerProvider>
  );
}
```

### Avoid shell components

Do not create a component that only arranges feature components and provides no state, logic, behaviour, or meaningful reusable abstraction. Keep one-off layout composition in the route's `page.tsx`, which makes the page structure visible at its entry point. A shared layout component is appropriate when the layout itself is reused or owns real layout behaviour.

```tsx
// Avoid: components/tools/schedule-timeline/root.tsx
export function ScheduleTimelineRoot() {
  return (
    <ToolPageLayout>
      <ScheduleTimelineToolbar />
      <ScheduleTimelineGrid />
    </ToolPageLayout>
  );
}

// Avoid: app/(printable)/tools/schedule-timeline/page.tsx
export default function ScheduleTimelinePage() {
  return <ScheduleTimelineRoot />;
}

// Prefer: keep the one-off composition on the page itself.
export default function ScheduleTimelinePage() {
  return (
    <ToolPageLayout>
      <ScheduleTimelineToolbar />
      <ScheduleTimelineGrid />
    </ToolPageLayout>
  );
}
```

### Avoid prop drilling

Passing a value to a direct child is fine. If intermediate components only forward feature state, use the feature's context or existing state management instead.

```tsx
// Avoid: Toolbar does not use selectedRunId.
<Schedule selectedRunId={selectedRunId} />;
function Schedule({ selectedRunId }: Props) {
  return <Toolbar selectedRunId={selectedRunId} />;
}
function Toolbar({ selectedRunId }: Props) {
  return <RunActions selectedRunId={selectedRunId} />;
}

// Prefer: the component that needs the state reads it.
function RunActions() {
  const { selectedRunId } = useSchedule();
  // ...
}
```

### Put helpers beside the feature

Do not accumulate helper functions above a component. Put non-trivial helpers in a `_utils.ts` file in the same directory, and put their tests beside the feature when appropriate.

```tsx
// Avoid: route-listing.tsx
function sortStops(stops: Stop[]) {
  return stops.toSorted((a, b) => a.sequence - b.sequence);
}

export function RouteListing() {
  // ...
}

// Prefer: _utils.ts
export function sortStops(stops: Stop[]) {
  return stops.toSorted((a, b) => a.sequence - b.sequence);
}
```

### Make branching explicit

Do not use nested ternary chains for business logic, user-facing copy, or error handling. Use a named helper with a `switch`, lookup map, or clear `if`/`else` blocks when mapping states to values.

```tsx
// Avoid
const label = isLoading ? "Loading" : error ? "Failed" : isEmpty ? "No runs" : "Ready";

// Prefer
function getScheduleLabel(state: ScheduleState) {
  switch (state) {
    case "loading":
      return "Loading";
    case "error":
      return "Failed";
    case "empty":
      return "No runs";
    case "ready":
      return "Ready";
  }
}
```

### Use boolean guards for optional JSX

When there is no meaningful `else` branch, do not use `condition ? <Component /> : null`. Name non-trivial conditions and render with `&&`.

```tsx
// Avoid
{run.status === "delayed" && run.delayMinutes > 0 ? <DelayWarning /> : null}

// Prefer
const shouldShowDelayWarning = run.status === "delayed" && run.delayMinutes > 0;

{shouldShowDelayWarning && <DelayWarning />}
```

### Extract repeated JSX shells

Do not copy the same label, control, card, or table-row structure. Extract the repeated shell into a small named component.

```tsx
// Avoid
<div className="space-y-1"><Label>Start</Label><TimeInput value={start} /></div>
<div className="space-y-1"><Label>Finish</Label><TimeInput value={finish} /></div>

// Prefer
function TimeField({ label, value }: TimeFieldProps) {
  return <div className="space-y-1"><Label>{label}</Label><TimeInput value={value} /></div>;
}

<TimeField label="Start" value={start} />
<TimeField label="Finish" value={finish} />
```

### Use named event handlers for non-trivial work

Inline handlers are appropriate for a single obvious action. Use a named handler when an event involves parsing, control flow, state coordination, or multiple statements.

```tsx
// Avoid
<Input onChange={(event) => {
  const minutes = Number.parseInt(event.target.value, 10);
  if (Number.isNaN(minutes)) return;
  setDelay(minutes);
  clearValidationError();
}} />

// Prefer
function handleDelayChange(event: ChangeEvent<HTMLInputElement>) {
  const minutes = Number.parseInt(event.target.value, 10);
  if (Number.isNaN(minutes)) return;

  setDelay(minutes);
  clearValidationError();
}

<Input onChange={handleDelayChange} />
```

### Read context actions where they are used

Do not pass provider-owned actions through intermediate components. The component performing the action should read it from context. A reusable leaf may still accept a callback when intentionally designed to be context-agnostic.

```tsx
// Avoid: ScheduleToolbar only forwards selectRun.
function Schedule() {
  const { selectRun } = useSchedule();
  return <ScheduleToolbar selectRun={selectRun} />;
}

function ScheduleToolbar({ selectRun }: Props) {
  return <RunSelector onSelect={selectRun} />;
}

// Prefer
function RunSelector() {
  const { selectRun } = useSchedule();
  return <Select onValueChange={selectRun} />;
}
```

### Build filtered collections directly

Avoid mapping entries to `null` and filtering afterward. Use a loop or reducer that only adds valid results.

```ts
// Avoid
const features = nodes
  .map((node) => node.position ? toFeature(node) : null)
  .filter((feature): feature is NodeFeature => feature !== null);

// Prefer
const features: NodeFeature[] = [];
for (const node of nodes) {
  if (node.position) {
    features.push(toFeature(node));
  }
}
```

### Prefer reviewable code over compact code

Optimize for code that is easy to review and maintain. Introduce names and intermediate steps when a compact expression hides intent.

```ts
// Avoid
const activeIds = new Set(runs.filter((run) => run.active && !run.cancelled).map(({ id }) => id));

// Prefer
const activeRuns = runs.filter((run) => run.active && !run.cancelled);
const activeRunIds = activeRuns.map((run) => run.id);
const activeIds = new Set(activeRunIds);
```
