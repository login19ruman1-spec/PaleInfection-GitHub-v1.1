# PaleInfection-GitHub-v1.1
# PaleInfection

Paper 1.21.4 plugin about an expanding pale-moss infection.

## What it does

- A **Heart** starts an infection zone.
- Pale moss spreads gradually from the Heart.
- The Heart glows, pulses orange particles into the ground, then grows a pale-oak shell/tree around itself.
- Players inside an infected zone receive **Скрипучий эффект** (tracked with the `pale_scritching` scoreboard tag and implemented through Nausea/Darkness/Blindness plus visual hallucinations).
- Four tiers of **Скрипуны** appear: Обычный, Охотник, Страж, Гений.
- Hallucinations include red spider eyes, silhouettes, false footsteps and short whispers.
- A **redstone block** suppresses growth within 5 blocks of it.
- Destroying a Heart stops and cleans its infection zone.
- Hearts are persisted in `plugins/PaleInfection/hearts.yml`.

## Build

Requires Java 21 and Maven.

```bash
mvn clean package
```

The jar appears in `target/PaleInfection-1.0.0.jar`.

## Commands

```text
/pale start      - start a new Heart at your location
/pale stop       - remove the nearest Heart and its infection
/pale status     - show nearest Heart and infection radius
/pale reload     - reload config
```

## Notes

The plugin does **not** change the actual Minecraft biome registry. It uses persistent Heart-centered infection zones, which is much lighter and reversible on a live Paper server.

The project targets the Paper 1.21.4 API. Paper's API Javadocs and development docs are the reference for the API version used here.


## Биом заражения

При создании сердца область вокруг него постепенно переводится в `PALE_GARDEN`. Плагин сохраняет исходный biome для каждой затронутой координаты в `hearts.yml`. При уничтожении сердца блоки заражения удаляются, а сохранённые биомы восстанавливаются. Это работает и после перезапуска сервера.

Дополнительно заражённая территория получила атмосферные эффекты: бледный пепел, оранжевые споры, листья, звуки сердца, а у игрока — постепенное усиление Slowness/Mining Fatigue и визуальных иллюзий.
