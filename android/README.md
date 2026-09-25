# Kur aš? – Android

Android programėlė rodo tą pačią „Kur aš?“ sąsają, kuri atidaroma „TyliaiTPk“ svetainėje. Žemėlapis, oras, adreso paieška, kompaso režimas ir gyvas bendrinimas veikia per saugų svetainės adresą `https://kur-as.t0m45-p4k0.chatgpt.site/app.html`. Todėl programėlei reikia interneto, o svetainėje paskelbti sąsajos pakeitimai matomi ir APK.

Telefono Android dalis į tos pačios sąsajos korteles papildomai įrašo matomų ir vietai naudojamų GNSS palydovų skaičių, vidutinį naudojamų palydovų C/N₀ (dB-Hz), tinklo tipą, mobiliojo signalo lygį ir dBm (kai telefonas pateikia), baterijos įkrovą. Įjungus gyvą bendrinimą programėlės ekrane, vietos paslauga siunčia šiuos rodmenis ir užrakintame telefone. Ji paleidžiama tik turint vietos leidimą ir rodo pranešimą, iš kurio galima nutraukti bendrinimą. Jei nutraukimo metu nėra interneto, paslauga bando panaikinti nuorodą, kai ryšys grįžta.

0.4.0 versijoje paslauga, kai sistema ją nutraukia, prašo automatinio atkūrimo ir išsaugo paskutinio sėkmingo perdavimo laiką. Ekrane ir nuolatiniame Android pranešime galima tikrinti, ar duomenys iš tiesų pasiekė serverį. Telefonas gali apriboti foninį GPS ir tinklą taupydamas energiją; jei siuntimas ilgai neatnaujinamas, telefono programėlės baterijos nustatymuose pasirinkite neribojamą veikimą. Po telefono perkrovimo ar priverstinio sustabdymo programėlę reikia atidaryti iš naujo ir patikrinti siuntimo būseną.

Paspaudus „Pradėti gyvą bendrinimą“ sukuriama stebėjimo nuoroda į `/app.html#watch=…`. Kitas asmuo gali atverti ją įdiegtoje „Kur aš?“ programėlėje arba naršyklėje: ta pati pagrindinė sąsaja rodo bendrinančio telefono vietą, greitį, kryptį, tikslumą, bateriją, GPS ir tinklo duomenis. Adresas bei oras gaunami pagal bendrinamas koordinates. Susiejimas išsaugomas stebėtojo telefone, kol jis paspaudžia „Grįžti į savo vietą“ arba bendrintojas nutraukia bendrinimą. Senesnės `/watch.html#…` nuorodos taip pat nukreipiamos į pilną stebėjimo vaizdą. Be įdiegto APK nuoroda veikia naršyklėje; Android patikrina svetainės sąsają su konkretaus APK parašu, prieš atidarydamas nuorodą programėlėje.

## Įdiegimas ir paleidimas

Atidarykite `android` katalogą per Android Studio su JDK 17 arba atsisiųskite „GitHub Actions“ scenarijaus **Android APK** artefaktą `kur-as-debug-apk`. Palaikoma Android 8.0 ir naujesnė versija. Paspaudus „Rodyti mano vietą“ prašoma vietos ir telefono būsenos leidimų; atskirai leidžiama juos pakeisti telefono nustatymuose. Jei neleidžiama tiksli vieta, GNSS palydovų rodmenų gali nebūti.

APK yra derinimo versija. Kiekvienas CI surinkimas gali naudoti kitą derinimo raktą, todėl norint įdiegti kitą APK versiją gali reikėti pašalinti ankstesnę. Projekte nėra Gradle wrapper failų; CI įdiegia Gradle 8.11.1 ir surenka `:app:assembleDebug`.
Svetainės `.well-known/assetlinks.json` turi atitikti konkretaus APK pasirašymo sertifikato SHA-256. Būsimiems patikimiems atnaujinimams būtinas pastovus pasirašymo raktas ir naujo sertifikato susiejimas su svetaine.

Prieš pašalinant ankstesnę derinimo versiją, būtina nutraukti joje veikiančią bendrinimo sesiją: pašalinus programėlę jos vietinis sustabdymo raktas prarandamas. Foninis siuntimas neprasideda automatiškai po telefono perkrovimo ar priverstinio programėlės sustabdymo; atidarius programėlę bendrinimas gali būti tęsiamas iš išsaugotos sesijos.
