# Kur aš? – Android

Android programėlė rodo tą pačią „Kur aš?“ sąsają, kuri atidaroma „TyliaiTPk“ svetainėje. Žemėlapis, oras, adreso paieška, kompaso režimas ir gyvas bendrinimas veikia per saugų svetainės adresą `https://kur-as.t0m45-p4k0.chatgpt.site/app.html`. Todėl programėlei reikia interneto, o svetainėje paskelbti sąsajos pakeitimai matomi ir APK.

Telefono Android dalis į tos pačios sąsajos korteles papildomai įrašo matomų ir vietai naudojamų GNSS palydovų skaičių, vidutinį naudojamų palydovų C/N₀ (dB-Hz), tinklo tipą, mobiliojo signalo lygį ir dBm (kai telefonas pateikia), baterijos įkrovą. Ji pati vietos į serverį nesiunčia – gyvas bendrinimas įjungiamas programėlės ekrane taip pat kaip naršyklėje.

Kito asmens gauta `https://kur-as.t0m45-p4k0.chatgpt.site/watch.html#…` stebėjimo nuoroda gali atsidaryti tiesiai „Kur aš?“ programėlėje, jei ji įdiegta ir Android patvirtino svetainės sąsają su APK parašu. Stebėjimo ekrane rodomi bendrinančio žmogaus duomenys; atidarius programėlę įprastai, rodoma paties telefono vieta. Be programėlės nuoroda ir toliau veikia naršyklėje.

## Įdiegimas ir paleidimas

Atidarykite `android` katalogą per Android Studio su JDK 17 arba atsisiųskite „GitHub Actions“ scenarijaus **Android APK** artefaktą `kur-as-debug-apk`. Palaikoma Android 8.0 ir naujesnė versija. Paspaudus „Rodyti mano vietą“ prašoma vietos ir telefono būsenos leidimų; atskirai leidžiama juos pakeisti telefono nustatymuose. Jei neleidžiama tiksli vieta, GNSS palydovų rodmenų gali nebūti.

APK yra derinimo versija. Kiekvienas CI surinkimas gali naudoti kitą derinimo raktą, todėl norint įdiegti kitą APK versiją gali reikėti pašalinti ankstesnę. Projekte nėra Gradle wrapper failų; CI įdiegia Gradle 8.11.1 ir surenka `:app:assembleDebug`.
Svetainės `.well-known/assetlinks.json` turi atitikti konkretaus APK pasirašymo sertifikato SHA-256. Būsimiems patikimiems atnaujinimams būtinas pastovus pasirašymo raktas ir naujo sertifikato susiejimas su svetaine.
