# Kur aš? – Android

Atskira Android programėlė dabartinei vietai ir ryšio būsenai matyti.

## Funkcijos
- OpenStreetMap žemėlapis ir buvimo vietos žymeklis.
- Koordinatės, vietos nustatymo tikslumo įvertis ir matavimo laikas.
- Matomų ir vietos fiksavimui naudojamų GNSS palydovų skaičius bei vidutinis naudojamų palydovų C/N₀ (dB-Hz).
- Wi-Fi arba mobiliųjų duomenų būklė, mobiliojo signalo lygis (0–4) ir dBm, jei telefonas pateikia.
- Apytikslis adresas (jei telefono geokoderis jį randa), telefono modelis, Android versija ir baterija.
- Mygtukai grįžti prie savo vietos ir bendrinti OpenStreetMap nuorodą.
- Gyvo bendrinimo mygtukas atveria naršyklės versiją; joje galima įjungti vietos siuntimą kitiems iki išjungimo. Android programėlė atskirai fone vietos nesiunčia.
- Vietos informacija nesiunčiama į programėlės serverį. Žemėlapio plytelės gaunamos internetu iš OpenStreetMap; bendrinimas vyksta tik naudotojui paspaudus mygtuką.

## Paleidimas
Atidarykite katalogą `android` su Android Studio (JDK 17), leiskite Gradle sinchronizuoti projektą ir paleiskite telefone su Android 8.0 ar naujesne versija. Suteikite vietos ir telefono būsenos leidimus. Žemėlapiui reikia interneto. Palydovų rodmenims būtinas tikslios vietos leidimas; jie gali būti neprieinami pastate ar jei GPS išjungtas. C/N₀ nėra vientisas telefono GPS kokybės procentas. Tikslus signalas priklauso nuo telefono ir SIM, o dviejų SIM atveju rodoma numatytos prenumeratos informacija.

Projekte nėra Gradle wrapper failų; Android Studio gali naudoti savo Gradle arba sugeneruoti wrapper įprastu `gradle wrapper` būdu. Kol kas APK nesukurtas.
