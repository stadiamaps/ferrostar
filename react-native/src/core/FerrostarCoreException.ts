class FerrostarCoreException extends Error {
  constructor(message: string) {
    super(message);
  }
}

export class NoResponseBodyException extends FerrostarCoreException {
  constructor() {
    super('Route request was successful but had no body.');
  }
}

export class NoRequestBodyException extends FerrostarCoreException {
  constructor() {
    super('Route request was not successful and had no request body.');
  }
}

export class InvalidStatusCodeException extends FerrostarCoreException {
  constructor(statusCode: number) {
    super(
      `Route request was not successful and had status code ${statusCode}.`
    );
  }
}
