# Kur aš?

Saugykloje yra dvi programėlės versijos: dabartinė naršyklės versija ir „Android“ programėlė aplanke `android/`. „Android“ programėlė atveria tą pačią sąsają ir papildomai joje rodo telefono GNSS bei mobiliojo ryšio rodmenis.

Telefono naršyklei pritaikyta vietos programėlė. Rodo vietą žemėlapyje, tikslumą, apytikslį adresą, greitį, judėjimo kryptį, orą ir naršyklės pateikiamą baterijos bei interneto būseną. Kompaso mygtuku galima perjungti šiaurę viršuje arba judėjimo kryptį viršuje. Koordinates galima atverti ir bendrinti per „Google Maps“ ar „Waze“.

Rankiniu būdu pajudinus žemėlapį ar pakeitus mastelį, automatinis sekimas ir pagal greitį keičiamas mastelis sustoja iki mygtuko „Kur aš?“ paspaudimo. Judant žemėlapyje rodoma raudona trajektorija, kuri pamažu išblunka per maždaug tris minutes. Ji saugoma tik atidarytoje peržiūroje; stebėtojas mato trajektoriją nuo tada, kai atvėrė programėlę. Kelio ašies ir piketažo funkcijos yra atskiroje „Ašis“ programėlėje.

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
- `public/watch.html` – gyvos vietos stebėjimas pagal nuorodą.
- `app/api/share/route.ts` – bendrinimo API.
- `public/vendor/` – vietinės žemėlapio bibliotekų kopijos.
- `drizzle/0000_broken_storm.sql` – bendrinimo duomenų bazės schema.
- `android/` – atskira „Android Studio“ programėlė (paleidimo aprašas `android/README.md`).
- `.openai/hosting.json` – svetainės talpinimo nustatymai.

Veikianti versija: https://kur-as.t0m45-p4k0.chatgpt.site/app.html
