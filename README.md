# Kokotmetr

> Reaction time meter with Czech insult-based scoring. Java app for CrEme J2ME / Windows CE.

Humorná Java aplikace pro měření reakčního času s patřičným hodnocením výsledku.
Navržena pro běh na CrEme J2ME (Windows CE, Personal Profile).

## O projektu

Stiskni tlačítko, počkej, stiskni znovu – a zjisti, jaký jsi kokot.

- Dvě vizualizace: lineární lišta nebo ručičkový ciferník
- Barevně kódovaný výsledek podle naměřeného času
- Hodnocení od „Normální borec" po „MEGAPRACURÁK"

| Čas | Hodnocení |
|---|---|
| < 1 s | Normální borec |
| < 2 s | Mírný kokot |
| < 4 s | Kokot |
| < 5 s | Těžký kokot |
| < 6 s | Kurjevský kokot |
| < 8 s | Pracurák |
| ≥ 8 s | MEGAPRACURÁK |

## Požadavky

- Cílová platforma: **CrEme J2ME** (Windows CE) – Java 1.4 compatible
- Sestavení vyžaduje Java 6 (`/opt/java6/bin/javac`)
- Spuštění na desktopu: Java 1.4+

## Sestavení

```bash
bash build.sh
```

Výstup: `out/Kokotmetr.jar`

## Spuštění

**Linux:**
```bash
bash run.sh
```

**Windows:**
```bat
run.bat
```

## Ovládání

| Vstup | Akce |
|---|---|
| `1` | Přepnout na lineární lištu |
| `2` / `↑` / `↓` | Start / stop měření / přepnutí na ciferník |
| Tlačítko dole | Přepnutí vizualizačního módu |
| Klik myší | Zaměření okna |

## Struktura projektu

```
src/Kokotmetr.java   – zdrojový kód aplikace
assets/              – obrázky pozadí
build.sh             – skript pro sestavení
run.sh / run.bat     – spouštěče
coords.py            – pomocný nástroj pro kalibraci obrázků (Python 3 + Pillow)
```

## Assets / Obrázky

All images in this repository were generated using AI tools
and do not intentionally replicate any existing copyrighted material.

## Licence

MIT – viz [LICENSE](LICENSE)
