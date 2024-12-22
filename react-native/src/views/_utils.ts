type UnitStyle = 'short' | 'long';

type DurationUnit = 'days' | 'hours' | 'minutes' | 'seconds';

type CalculatedResult = {
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
};

export const LocalizedDurationFormatter = () => {
  const units: DurationUnit[] = ['days', 'hours', 'minutes', 'seconds'];
  const unitStyle: UnitStyle = 'short';

  function calculate(durationSeconds: number): CalculatedResult {
    let remainingDuration = durationSeconds;
    const result: CalculatedResult = {
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0,
    };

    // Extract the days from the duration
    if (units.find((u) => u === 'days')) {
      const days = parseInt(
        (remainingDuration / (24 * 60 * 60)).toFixed(0),
        10
      );
      remainingDuration %= 24 * 60 * 60;
      result.days = days;
    }

    // Extract the hours from the duration
    if (units.find((u) => u === 'hours')) {
      const hours = parseInt((remainingDuration / (60 * 60)).toFixed(0), 10);
      remainingDuration %= 60 * 60;
      result.hours = hours;
    }

    // Extract the minutes from the duration
    if (units.find((u) => u === 'minutes')) {
      const minutes = parseInt((remainingDuration / 60).toFixed(0), 10);
      remainingDuration %= 60;
      result.minutes = minutes;
    }

    // Extract the seconds from the duration
    if (units.find((u) => u === 'seconds')) {
      const seconds = parseInt(remainingDuration.toFixed(0), 10);
      result.seconds = seconds;
    }

    return result;
  }

  function getUnitString(unit: DurationUnit, value: number): string {
    const plural = value != 1 ? 's' : '';

    switch (unitStyle) {
      case 'short':
        switch (unit) {
          case 'seconds':
            return 's';
          case 'minutes':
            return 'm';
          case 'hours':
            return 'h';
          case 'days':
            return 'd';
        }
        break;
      case 'long':
        switch (unit) {
          case 'seconds':
            return `${value} ${unit}${plural}`;
          case 'minutes':
            return `${value} ${unit}${plural}`;
          case 'hours':
            return `${value} ${unit}${plural}`;
          case 'days':
            return `${value} ${unit}${plural}`;
        }
        break;
      default:
        return '';
    }
  }

  function format(durtionSeconds: number): string {
    const durationRecord = calculate(durtionSeconds);

    console.log(Object.entries(durationRecord));
    return Object.entries(durationRecord)
      .filter((it) => it[1] > 0)
      .flatMap((it) => `${it[1]}${getUnitString(it[0] as DurationUnit, it[1])}`)
      .join(' ');
  }

  return {
    format,
  };
};
