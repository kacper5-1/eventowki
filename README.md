# Eventowki (Paper 1.21+, Java 21)

Budowanie: `mvn package` -> `target/Eventowki-1.0.0.jar` do folderu `plugins/`.

Komendy (permisja `eventowki.admin`):
- `/eventowka give <gracz> <id> [ilosc]` - np. `/eventowka give Jan pole_smierci 3`
- `/eventowka list`
- `/eventowka reload`

Pole Smierci: PPM stawia strefe wokol gracza. Kazdy w srodku nie moze uzyc elytry
(ani odpalic fajerwerka na elytrze), a po wyjsciu z pola blokada trwa jeszcze
`block-after-leave-seconds` (domyslnie 5 s).
Wszystko (promien, czas, cooldown, nazwa, material) ustawisz w config.yml.

Zamek z Piasku: PPM buduje wokol gracza zamknieta bryle (podloga + mury + dach + krenelaz) z piasku,
ktory znika po `duration-seconds`. Nie da sie wejsc do srodka: dach zamyka gore, wszystko jest
niezniszczalne (takze dla wlasciciela), odporne na wybuchy i tloki, a perla/chorus nie wpuszczaja
nikogo do srodka. Wszystko wylaczalne w config.yml (`castle-*`).

Dodanie kolejnej eventowki: nowy wpis w `items:` w config.yml + nowy `type`
obsluzony w EventListener#onUse.
