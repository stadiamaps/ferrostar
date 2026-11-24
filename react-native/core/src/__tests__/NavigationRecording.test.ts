import { readFileSync } from 'node:fs';
import { decode } from '@msgpack/msgpack';
import { expect, it } from 'vitest';

it('decodes Rust MessagePack recordings using the JSON schema', () => {
  // The Rust test pins this fixture to NavigationRecordingMetadata.to_msgpack output.
  const bytes = readFileSync(
    new URL(
      '../../../../common/ferrostar/src/fixtures/recording_canonical.msgpack',
      import.meta.url
    )
  );
  const expected = JSON.parse(
    readFileSync(
      new URL(
        '../../../../common/ferrostar/src/fixtures/recording_canonical.json',
        import.meta.url
      ),
      'utf8'
    )
  );

  const buffer = new Uint8Array(bytes).buffer;
  expect(decode(buffer)).toEqual(expected);
});
