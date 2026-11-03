import { useEffect, useState } from 'react';

const CITIES = [
  { name: 'Bengaluru', tz: 'Asia/Kolkata' },
  { name: 'Lagos', tz: 'Africa/Lagos' },
  { name: 'Lisbon', tz: 'Europe/Lisbon' },
  { name: 'São Paulo', tz: 'America/Sao_Paulo' },
  { name: 'Toronto', tz: 'America/Toronto' },
  { name: 'Singapore', tz: 'Asia/Singapore' },
];

function parts(date, tz) {
  const f = new Intl.DateTimeFormat('en-GB', { timeZone: tz, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
  const [h, m] = f.format(date).split(':');
  return { h, m, day: Number(h) >= 7 && Number(h) < 19 };
}

/** Remote work is about time zones, so the landing page shows real local times, live. */
export default function WorldClock() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 15000);
    return () => clearInterval(t);
  }, []);

  return (
    <ul className="m-0 grid list-none grid-cols-2 gap-x-6 gap-y-5 p-0 sm:grid-cols-3 lg:grid-cols-6" aria-label="Current local time in cities where our candidates and startups are">
      {CITIES.map((c) => {
        const { h, m, day } = parts(now, c.tz);
        return (
          <li key={c.tz} className="border-t-2 border-ink pt-2">
            <div className="font-display text-3xl font-bold tabular">
              {h}<span className="clock-colon">:</span>{m}
            </div>
            <div className="mt-1 flex items-center gap-2 text-sm text-slate-700">
              <span
                aria-hidden="true"
                className="inline-block h-2.5 w-2.5 rounded-full"
                style={{ background: day ? '#FFB547' : '#0F1B2D' }}
              />
              {c.name}
              <span className="sr-only">{day ? ', daytime' : ', night'}</span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
