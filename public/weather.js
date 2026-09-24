(function () {
  function describe(code, isDay) {
    if (code === 0) return { icon: isDay ? '☀️' : '🌙', label: 'Giedra' };
    if (code === 1 || code === 2) return { icon: isDay ? '🌤️' : '🌙', label: 'Mažai debesuota' };
    if (code === 3) return { icon: '☁️', label: 'Debesuota' };
    if (code === 45 || code === 48) return { icon: '🌫️', label: 'Rūkas' };
    if ([51, 53, 55, 56, 57].includes(code)) return { icon: '🌦️', label: 'Dulksna' };
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return { icon: '🌧️', label: 'Lietus' };
    if ([71, 73, 75, 77, 85, 86].includes(code)) return { icon: '🌨️', label: 'Sniegas' };
    if ([95, 96, 99].includes(code)) return { icon: '⛈️', label: 'Perkūnija' };
    return { icon: '🌡️', label: 'Oras' };
  }

  async function current(latitude, longitude) {
    // Weather models do not require the phone's exact GPS coordinates.
    const query = new URLSearchParams({
      latitude: Number(latitude).toFixed(2),
      longitude: Number(longitude).toFixed(2),
      current: 'temperature_2m,weather_code,is_day',
      temperature_unit: 'celsius',
      forecast_days: '1'
    });
    const response = await fetch('https://api.open-meteo.com/v1/forecast?' + query);
    if (!response.ok) throw new Error('Weather unavailable');
    const data = (await response.json()).current;
    if (!data || !Number.isFinite(data.temperature_2m)) throw new Error('Weather unavailable');
    return { ...describe(data.weather_code, data.is_day === 1), temperature: Math.round(data.temperature_2m) };
  }

  window.KurAsWeather = { current };
})();
