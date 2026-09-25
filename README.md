# Kur aš?

Saugykloje yra dvi programėlės versijos: dabartinė naršyklės versija ir „Android“ programėlė aplanke `android/`. „Android“ programėlė atveria tą pačią sąsają ir papildomai joje rodo telefono GNSS bei mobiliojo ryšio rodmenis.

Telefono naršyklei pritaikyta vietos programėlė. Rodo vietą žemėlapyje, tikslumą, apytikslį adresą, greitį, judėjimo kryptį, orą ir naršyklės pateikiamą baterijos bei interneto būseną. Kompaso mygtuku galima perjungti šiaurę viršuje arba judėjimo kryptį viršuje. Koordinates galima atverti ir bendrinti per „Google Maps“ ar „Waze“.

Rankiniu būdu pajudinus žemėlapį ar pakeitus mastelį, automatinis sekimas ir pagal greitį keičiamas mastelis sustoja iki mygtuko „Kur aš?“ paspaudimo. Ašį galima įkelti iš Civil 3D LandXML (`.xml`) su `Alignment / CoordGeom` (tiesėmis, apskritiminėmis kreivėmis ir klotoidėmis), WGS84 GeoJSON `LineString`, GPX (`rtept` arba `trkpt`) arba CSV failo su `lat,lon` antrašte. LandXML atpažįsta LKS94 (X,Y) ar WGS84 ir skaito `staStart`; jei faile kelios ašys, galima pasirinkti norimą. Piketažo lūžių (`StaEquation`), kitų projekcijų ir kitų perėjimo kreivių neinterpretuoja, kad nerodytų klaidinančios vietos. Žemėlapyje ašis rodoma su šimto metrų piketų žymomis priartinus, o buvimo vietai skaičiuojamas artimiausias piketas kas 5 m ir atstumas iki ašies. Tikslumą riboja GPS, pasirinktą ašį saugo tik to įrenginio naršyklėje.

Įjungus gyvą bendrinimą sukuriama stebėjimo nuoroda. Vietos atnaujinimas veikia, kol naršyklė leidžia šiam puslapiui veikti; stebėtojas mato paskutinio atnaujinimo laiką. Bendrinimą galima bet kada nutraukti. Telefonas pats nustato naujų vietos matavimų dažnį ir judėjimo krypties prieinamumą.

## Paleidimas

Reikia Node.js 22.13 ar naujesnės versijos.

```sh
git clone https://github.com/tomas-pak0/kur-as.git
cd kur-as
npm ci
npm run dev
```

Atverkite `http://localhost:5173/app.html`. Naršyklės geolokacijai reikia saugaus konteksto (HTTPS arba `localhost`) ir naudotojo leidimo. Patikrinimas: `npm run build`.

Gyvam bendrinimui reikia veikiančios Cloudflare D1 duomenų bazės, susietos vardu `DB`, ir lentelės iš `drizzle/0000_broken_storm.sql`. Be jos vietos žemėlapis veikia, tačiau naujo gyvo bendrinimo pradėti nepavyks. Ši saugykla yra išeities kodas; vien „GitHub Pages“ serverinės bendrinimo API nepaleidžia.

## Svarbiausi failai

- `public/app.html` – pagrindinė programėlė.
- `public/alignment.js` – ašies failo interpretavimas ir piketažo skaičiavimas.
- `public/watch.html` – gyvos vietos stebėjimas pagal nuorodą.
- `app/api/share/route.ts` – bendrinimo API.
- `public/vendor/` – vietinės žemėlapio bibliotekų kopijos.
- `drizzle/0000_broken_storm.sql` – bendrinimo duomenų bazės schema.
- `android/` – atskira „Android Studio“ programėlė (paleidimo aprašas `android/README.md`).
- `.openai/hosting.json` – svetainės talpinimo nustatymai.

Veikianti versija: https://kur-as.t0m45-p4k0.chatgpt.site/app.html
