export function getNanoTime(): number {
  const hrTime = process.hrtime();
  return hrTime[0] * 1000000000 + hrTime[1];
}

export function ab2json(ab: ArrayBuffer): object {
  return JSON.parse(
    String.fromCharCode.apply(null, Array.from(new Uint8Array(ab)))
  );
}
