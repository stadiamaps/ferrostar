export type AnnotationParser<T> = (value: unknown) => T;

export type AnnotationResult<T> = {
  data?: T;
  error?: Error;
};

/**
 * Decode the annotation JSON for the current route segment.
 *
 * Parsing is deliberately split into two steps: Ferrostar handles JSON decoding,
 * while the supplied parser validates and maps the unknown JSON value into the
 * application-specific annotation type.
 */
export function decodeAnnotation<T>(
  annotationJson: string | undefined,
  parser: AnnotationParser<T>
): AnnotationResult<T> {
  if (annotationJson === undefined) {
    return {};
  }

  try {
    return { data: parser(JSON.parse(annotationJson)) };
  } catch (cause) {
    return { error: toError(cause) };
  }
}

function toError(cause: unknown): Error {
  if (cause instanceof Error) {
    return cause;
  }

  return new Error(String(cause));
}
