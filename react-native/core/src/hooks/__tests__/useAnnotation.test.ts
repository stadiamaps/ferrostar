import { describe, expect, it, vi } from 'vitest';

vi.mock('@stadiamaps/ferrostar-uniffi-react-native', () => ({
  TripState: {
    Navigating: {
      instanceOf: (value: { tag?: string } | undefined) =>
        value?.tag === 'Navigating',
    },
  },
}));

import * as React from 'react';
import * as TestRenderer from 'react-test-renderer';
import type { FerrostarCore, NavigationState } from '../../FerrostarCore';
import type { AnnotationResult } from '../../annotations/Annotation';
import { FerrostarContext } from '../../contexts/FerrostarProvider';
import { useAnnotation } from '../useAnnotation';

(
  globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;

const parseName = (value: unknown): string => {
  if (
    typeof value !== 'object' ||
    value === null ||
    !('name' in value) ||
    typeof value.name !== 'string'
  ) {
    throw new TypeError('name must be a string');
  }

  return value.name;
};

class FakeCore {
  _state: NavigationState;
  private listener?: (state: NavigationState) => void;
  removeStateListener = vi.fn();

  constructor(annotationJson?: string) {
    this._state = navigatingState(annotationJson);
  }

  addStateListener(listener: (state: NavigationState) => void): number {
    this.listener = listener;
    return 7;
  }

  emit(annotationJson?: string): void {
    this._state = navigatingState(annotationJson);
    this.listener?.(this._state);
  }
}

function navigatingState(annotationJson?: string): NavigationState {
  return {
    tripState: {
      tag: 'Navigating',
      inner: { annotationJson },
    },
  } as NavigationState;
}

type ProbeProps = {
  onResult: (result: AnnotationResult<string>) => void;
};

function Probe({ onResult }: ProbeProps) {
  onResult(useAnnotation(parseName));
  return null;
}

describe('useAnnotation', () => {
  it('decodes the current annotation and follows state updates', async () => {
    const core = new FakeCore('{"name":"first"}');
    const results: Array<AnnotationResult<string>> = [];

    let renderer!: TestRenderer.ReactTestRenderer;
    await TestRenderer.act(async () => {
      renderer = TestRenderer.create(
        React.createElement(
          FerrostarContext.Provider,
          { value: { core: core as unknown as FerrostarCore } },
          React.createElement(Probe, {
            onResult: (result) => results.push(result),
          })
        )
      );
    });

    expect(results.at(-1)).toEqual({ data: 'first' });

    await TestRenderer.act(async () => {
      core.emit('{"name":"second"}');
    });

    expect(results.at(-1)).toEqual({ data: 'second' });

    await TestRenderer.act(async () => {
      core.emit('{"name":42}');
    });

    expect(results.at(-1)?.data).toBeUndefined();
    expect(results.at(-1)?.error).toEqual(
      new TypeError('name must be a string')
    );

    await TestRenderer.act(async () => {
      renderer.unmount();
    });

    expect(core.removeStateListener).toHaveBeenCalledWith(7);
  });
});
