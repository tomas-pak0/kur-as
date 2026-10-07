# Kur aš? – Android

Android programėlė rodo tą pačią „Kur aš?“ sąsają, kuri atidaroma „TyliaiTPk“ svetainėje. Žemėlapis, oras, adreso paieška, kompaso režimas ir gyvas bendrinimas veikia per saugų svetainės adresą `https://kur-as.t0m45-p4k0.chatgpt.site/app.html`. Todėl programėlei reikia interneto, o svetainėje paskelbti sąsajos pakeitimai matomi ir APK.

Telefono Android dalis į tos pačios sąsajos korteles papildomai įrašo matomų ir vietai naudojamų GNSS palydovų skaičių, vidutinį naudojamų palydovų C/N₀ (dB-Hz), tinklo tipą, mobiliojo signalo lygį ir dBm (kai telefonas pateikia), baterijos įkrovą. Įjungus gyvą bendrinimą programėlės ekrane, vietos paslauga siunčia šiuos rodmenis ir užrakintame telefone. Ji paleidžiama tik turint vietos leidimą ir rodo pranešimą, iš kurio galima nutraukti bendrinimą. Jei nutraukimo metu nėra interneto, paslauga bando panaikinti nuorodą, kai ryšys grįžta.

0.4.0 versijoje paslauga, kai sistema ją nutraukia, prašo automatinio atkūrimo ir išsaugo paskutinio sėkmingo perdavimo laiką. 0.4.1 versijoje pridėtas failų pasirinkimo langas LandXML ašims įkelti tiesiai iš telefono. Ekrane ir nuolatiniame Android pranešime galima tikrinti, ar duomenys iš tiesų pasiekė serverį. Telefonas gali apriboti foninį GPS ir tinklą taupydamas energiją; jei siuntimas ilgai neatnaujinamas, telefono programėlės baterijos nustatymuose pasirinkite neribojamą veikimą. Po telefono perkrovimo ar priverstinio sustabdymo programėlę reikia atidaryti iš naujo ir patikrinti siuntimo būseną.

0.4.2 versijoje Android dokumentų parinkiklis rodo visus dokumentus, nes skirtingos failų programos XML dokumentus priskiria skirtingiems tipams. Pasirinkus failą programėlė patikrina jo plėtinį ir LandXML turinį. Ieškokite failo telefono „Failai“ / „Atsisiuntimai“ aplanke; programėlė neskaito pasirinktų failų iš kito telefono.

Paspaudus „Pradėti gyvą bendrinimą“ sukuriama stebėjimo nuoroda į `/app.html#watch=…`. Kitas asmuo gali atverti ją įdiegtoje „Kur aš?“ programėlėje arba naršyklėje: ta pati pagrindinė sąsaja rodo bendrinančio telefono vietą, greitį, kryptį, tikslumą, bateriją, GPS ir tinklo duomenis. Adresas bei oras gaunami pagal bendrinamas koordinates. Susiejimas išsaugomas stebėtojo telefone, kol jis paspaudžia „Grįžti į savo vietą“ arba bendrintojas nutraukia bendrinimą. Senesnės `/watch.html#…` nuorodos taip pat nukreipiamos į pilną stebėjimo vaizdą. Be įdiegto APK nuoroda veikia naršyklėje; Android patikrina svetainės sąsają su konkretaus APK parašu, prieš atidarydamas nuorodą programėlėje.

## Įdiegimas ir paleidimas

Atidarykite `android` katalogą per Android Studio su JDK 17 arba atsisiųskite „GitHub Actions“ scenarijaus **Android APK** artefaktą `kur-as-debug-apk`. Palaikoma Android 8.0 ir naujesnė versija. Paspaudus „Rodyti mano vietą“ prašoma vietos ir telefono būsenos leidimų; atskirai leidžiama juos pakeisti telefono nustatymuose. Jei neleidžiama tiksli vieta, GNSS palydovų rodmenų gali nebūti.

APK yra derinimo versija. Kiekvienas CI surinkimas gali naudoti kitą derinimo raktą, todėl norint įdiegti kitą APK versiją gali reikėti pašalinti ankstesnę. Projekte nėra Gradle wrapper failų; CI įdiegia Gradle 8.11.1 ir surenka `:app:assembleDebug`.
Svetainės `.well-known/assetlinks.json` turi atitikti konkretaus APK pasirašymo sertifikato SHA-256. Būsimiems patikimiems atnaujinimams būtinas pastovus pasirašymo raktas ir naujo sertifikato susiejimas su svetaine.

Prieš pašalinant ankstesnę derinimo versiją, būtina nutraukti joje veikiančią bendrinimo sesiją: pašalinus programėlę jos vietinis sustabdymo raktas prarandamas. Foninis siuntimas neprasideda automatiškai po telefono perkrovimo ar priverstinio programėlės sustabdymo; atidarius programėlę bendrinimas gali būti tęsiamas iš išsaugotos sesijos.

## Sąsaja v0.5.0 (2026-10-06)

Bendra internetinė ir Android sąsaja turi LT, LV, PL, EN, DE, ES ir FR kalbas, išsaugomą kalbos pasirinkimą bei centruotą raudoną versijos eilutę. Vektorinis OpenFreeMap žemėlapis sukasi pagal judėjimo kryptį, o tekstas lieka horizontalus; kelio pavadinimai rodomi tiesiomis etiketėmis. Kryptis, sklandus mastelis ir centro judėjimas valdomi viena animacija. Palietus arba valdant žemėlapį automatinis sekimas sustabdomas, kol paspaudžiamas vietos mygtukas. Įrenginyje be WebGL naudojamas atsarginis OpenStreetMap žemėlapis su pranešimu, kad jo užrašai sukasi kartu su vaizdu.

Šiam sąsajos atnaujinimui nereikia naujo APK: esamas Android paketas įkelia `/app.html`. v0.5.0 yra sąsajos versija; Android paketo `versionName` nepakeistas. Šio atnaujinimo metu fizinio Android įrenginio GPS ir WebGL bandymas nebuvo atliktas. Automatinius sąsajos logikos patikrinimus vykdykite iš projekto šaknies: `node scripts/check-kur-as.mjs`.

## Sąsaja v0.5.2 (2026-10-06)

Pridėtos italų (IT) ir ukrainiečių (UK) kalbos; visos devynios kalbos su vėliavomis rodomos viename stulpelyje. Žemėlapio tekstas parenkamas pagal kalbą, pirmenybę teikiant išverstam, vėliau vietiniam pavadinimui. LT režime nebetaikoma numatytoji angliškų pavadinimų pirmenybė. Adreso duomenų angliški savivaldybių ir apskričių apibūdinimai pakeičiami lietuviškais; svetimų tikrinių vietovardžių programėlė neišgalvoja. Pakeitus kalbą pasenęs adreso atsakymas ignoruojamas.

Lietuvos ORT10LT sluoksnį pakeitė pasaulinis Esri World Imagery foto žemėlapis. Jis įjungiamas ta pačia varnele, išsaugomas pasirinkimas ir išlieka horizontalūs vektoriniai užrašai. Paslauga teikia palydovines bei aeronuotraukas; detalumas ir vaizdų datos priklauso nuo vietovės. Naujų GPS ar fizinio Android bandymų šiame atnaujinime neatlikta.

## Pasirašytas leidimas v0.5.3 (2026-10-07)

Android versionCode 10; compileSdk/targetSdk 36; AGP 8.10.1; JDK 17 ir Gradle 8.14.3. Dešimt kalbų (LT, LV, PL, EN, DE, ES, FR, IT, UK, RU). Pridėtas duomenų perdavimo dialogas prieš naują bendrinimą ir privatumo politikos nuoroda. Android pranešimo tekstai naudoja programėlėje pasirinktą kalbą. Pasirašymas naudoja KURAS_KEYSTORE ir KURAS_KEY_PASSWORD aplinkos kintamuosius; raktas nesaugomas Git. Surinkimas: gradle :app:assembleRelease :app:bundleRelease.

Šis leidimas turi naują pastovų upload raktą. Ankstesnio debug APK parašas nesutampa; jo negalima pakeisti šiuo APK tiesioginiu atnaujinimu. Prieš seno APK pašalinimą nutrauk bendrinimą. Vėlesnius tiesioginius APK pasirašyk tuo pačiu raktu. Google Play App Signing naudojamas atskiras Google platinimo raktas; jo SHA-256 būtina papildyti assetlinks.json prieš tikrinant Play įdiegtos programėlės nuorodas.

## v0.5.4 (2026-10-07)

Kalbų kodai išrikiuoti DE, EN, ES, FR, IT, LT, LV, PL, RU, UK. Android versionCode 11. Išlaikytas tas pats v0.5.3 upload pasirašymo raktas ir paketo ID, todėl v0.5.3 → v0.5.4 APK atnaujinimas gali išsaugoti programėlės duomenis. Release build be esamo pasirašymo rakto ar slaptažodžio sustabdomas. Seno debug parašo tęstinumas negalimas be seno privataus rakto; naujas raktas jo nepakeičia.


## 0.5.7

VersionCode 14; paketo ID ir upload sertifikatas išlaikyti. Ekranas palaikomas įjungtas, kol MainActivity matoma, ir sugrįžus į programėlę. Perėjus į foną ekranas gali užmigti; ShareService ir jos GPS bei HTTPS siuntimas veikia nepriklausomai nuo WebView. Pagalbos lange naudojamas tik esamos vietos šalies kodas ir dabartinio mobiliojo tinklo numeriai; namų SIM šalies numeris nenaudojamas kaip esamos šalies pakaitalas. Nustačius nepažįstamą šalį ir negavus telefono numerių, pateikiamas telefono rinkiklis. Skambinama per ACTION_DIAL, vietos tekstas bendrinamas per ACTION_SEND pasirenkant gavėją telefone.

Android Auto sąsaja šiame leidime nepridėta. Maršrutą galima atverti Google Maps arba Waze. Atskiram automobilio ekranui reikėtų Car App Library navigacijos arba POI sąsajos ir Android Auto kokybės patikros.

Automatinės logikos patikros: node scripts/check-kur-as.mjs, node scripts/check-background-sharing.mjs, node scripts/check-location-features.mjs. Android instrumentavimo testas tikrina ekrano budrumą ir naujos GPS vietos gavimą per API, programėlei veikiant fone. Jis skirtas tik emuliatoriui ir nesurinkinėja tikrų vartotojų vietos duomenų.
