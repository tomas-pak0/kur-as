# Kur aš?

Telefono naršyklei pritaikyta vietos programėlė. Rodo vietą žemėlapyje, tikslumą, apytikslį adresą, greitį, judėjimo kryptį, orą ir naršyklės pateikiamą baterijos bei interneto būseną. Kompaso mygtuku galima perjungti šiaurę viršuje arba judėjimo kryptį viršuje. Koordinates galima atverti ir bendrinti per „Google Maps“ ar „Waze“.

Įjungus gyvą bendrinimą sukuriama stebėjimo nuoroda. Vietos atnaujinimas veikia, kol naršyklė leidžia šiam puslapiui veikti; stebėtojas mato paskutinio atnaujinimo laiką. Bendrinimą galima bet kada nutraukti. Telefonas pats nustato naujų vietos matavimų dažnį ir judėjimo krypties prieinamumą.

## Paleidimas

Reikia Node.js 22.13 ar naujesnės versijos.

```sh
npm ci
npm run dev
```

Atverkite `/app.html` vietiniame kūrimo serveryje. Naršyklės geolokacijai reikia saugaus konteksto (HTTPS arba `localhost`) ir naudotojo leidimo. Gyvam bendrinimui taip pat reikia serverio D1 duomenų bazės `DB` susiejimo.

## Svarbiausi failai

- `public/app.html` – pagrindinė programėlė.
- `public/watch.html` – gyvos vietos stebėjimas pagal nuorodą.
- `app/api/share/route.ts` – bendrinimo API.
- `public/vendor/` – vietinės žemėlapio bibliotekų kopijos.
- `.openai/hosting.json` – svetainės talpinimo nustatymai.

Veikianti versija: https://kur-as.t0m45-p4k0.chatgpt.site/app.html
