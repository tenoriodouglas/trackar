import { describe, expect, it } from "vitest";
import { formatDistance, formatDuration, summarize, TripPoint } from "../src/lib/trip";

const t0 = Date.UTC(2026, 0, 1, 8);
const point = (minute: number, speedKmh: number, lat = 0, valid = true): TripPoint => ({
  lat,
  lng: 0,
  time: t0 + minute * 60_000,
  speedKmh,
  course: 0,
  valid,
});

describe("summarize", () => {
  it("matches the app's rules for distance, speed and moving time", () => {
    const summary = summarize([point(0, 50, 0), point(1, 70, 0.01), point(2, 0, 0.02)]);
    expect(summary.distanceKm).toBeCloseTo(2.22, 2);
    expect(summary.maxSpeedKmh).toBe(70);
    expect(summary.movingMs).toBe(2 * 60_000);
  });

  it("detects stops of five minutes or more, including a trailing one", () => {
    const summary = summarize([point(0, 40), point(1, 0), point(3, 0), point(4, 40), point(5, 0), point(20, 0), point(21, 30), point(22, 0), point(40, 0)]);
    expect(summary.stops.map((s) => (s.end - s.start) / 60_000)).toEqual([16, 18]);
  });

  it("ignores invalid fixes and handles empty input", () => {
    expect(summarize([point(0, 10), point(1, 300, 0, false)]).maxSpeedKmh).toBe(10);
    expect(summarize([]).stops).toEqual([]);
  });
});

describe("formatters", () => {
  it("format distance and duration in Portuguese", () => {
    expect(formatDistance(0.4)).toBe("400 m");
    expect(formatDistance(12.34)).toBe("12,3 km");
    expect(formatDuration(95 * 60_000)).toBe("1h 35min");
    expect(formatDuration(5 * 60_000)).toBe("5 min");
  });
});
