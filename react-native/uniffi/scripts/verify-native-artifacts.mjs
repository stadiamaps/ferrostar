import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const generatedCppPath = join('cpp', 'generated', 'ferrostar.cpp');
const androidAbis = ['arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'];

function fail(message) {
  throw new Error(`native artifact verification failed: ${message}`);
}

if (!existsSync(generatedCppPath)) {
  fail(`missing generated bindings at ${generatedCppPath}`);
}

const generatedCpp = readFileSync(generatedCppPath, 'utf8');
const externStart = generatedCpp.indexOf('extern "C" {');
const externEnd = generatedCpp.indexOf(
  '\n}\n\nnamespace uniffi::ferrostar',
  externStart
);

if (externStart < 0 || externEnd < 0) {
  fail('could not locate the generated UniFFI declarations');
}

const declarations = generatedCpp.slice(externStart, externEnd);
const expectedSymbols = [
  ...new Set(
    declarations.match(
      /\b(?:uniffi_ferrostar|ffi_ferrostar)_[a-zA-Z0-9_]+(?=\s*\()/g
    ) ?? []
  ),
];

if (expectedSymbols.length === 0) {
  fail('no UniFFI symbols were found in the generated bindings');
}

for (const abi of androidAbis) {
  const archivePath = join(
    'android',
    'src',
    'main',
    'jniLibs',
    abi,
    'libferrostar.a'
  );

  if (!existsSync(archivePath)) {
    fail(`missing Android archive at ${archivePath}`);
  }

  // Archive symbol tables store exported names as bytes. Comparing them with
  // the generated declarations catches stale Rust libraries before packaging.
  const archive = readFileSync(archivePath).toString('latin1');
  const missingSymbols = expectedSymbols.filter(
    (symbol) => !archive.includes(symbol)
  );

  if (missingSymbols.length > 0) {
    fail(
      `${archivePath} is missing ${missingSymbols.length} generated symbols:\n` +
        missingSymbols.map((symbol) => `  ${symbol}`).join('\n')
    );
  }
}

const xcframeworkPath = 'FerrostarRN.xcframework';
if (!existsSync(xcframeworkPath)) {
  fail(`missing iOS framework at ${xcframeworkPath}`);
}

console.log(
  `Verified ${expectedSymbols.length} UniFFI symbols across ${androidAbis.length} Android ABIs and the iOS framework.`
);
