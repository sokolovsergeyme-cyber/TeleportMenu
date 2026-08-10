# RaiderWorld

Плагин для Spigot **26.1.2** (Minecraft 26.1) на **Java 25**.  
Совместим с **Geyser** (Java + Bedrock / телефоны).

Периодические рейды волнами, BossBar, настраиваемые мобы, броня/оружие, ночь во время рейда, точка спавна или около игрока. Почти всё настраивается командами без правки кода.

## Сборка

Требования:
- JDK 25+
- Maven 3.9+

```bash
mvn clean package
```

Готовый jar появится в `target/RaiderWorld-1.0.0.jar`.  
Положи его в папку `plugins` сервера Spigot/Paper 26.1.2.

## Команды

| Команда | Описание |
|---------|----------|
| `/raid start [игрок]` | Запустить рейд около себя или указанного игрока |
| `/raid stop` | Остановить рейд (если `allow-cancel: true`) |
| `/raid setspawn` | Установить фиксированную точку рейда |
| `/raid status` | Текущий статус |
| `/raid reload` | Перезагрузить конфиг |
| `/raid settings waves <n>` | Количество волн |
| `/raid settings interval <дни>` | Интервал между рейдами |
| `/raid settings night true/false` | Принудительная ночь |
| `/raid settings radius <n>` | Радиус спавна мобов |
| `/raid settings difficulty HARD` | Сложность |
| `/raid settings allowcancel true/false` | Можно ли отменять рейд |
| `/raid settings mobs add <type> <minWave> <min-max>` | Добавить/изменить моба |
| `/raid settings mobs remove <type>` | Удалить моба |
| `/raid settings mobs armor <type> <0-100>` | Шанс брони |
| `/raid settings mobs weapon <type> <0-100>` | Шанс оружия |
| `/raid settings mobs list` | Список мобов |

**Примеры:**
```
/raid settings mobs add skeleton 1 15-30
/raid settings mobs add zombie 1 20-40
/raid settings mobs add husk 2 10-25
/raid settings waves 6
/raid settings interval 5
/raid settings night true
/raid settings radius 50
```

## Особенности

- BossBar показывает время до рейда / текущую волну / оставшихся мобов.
- Во время рейда — ночь (настраивается).
- Мобы появляются с бронёй и оружием по шансам (можно неполный комплект).
- На последней волне спавнится усиленный мини-босс.
- Зомби помечены тегом `raiderworld_breaker` — заготовка под улучшенный AI ломания блоков (для полной реализации рекомендуется Paper + Pathfinder API или NMS).
- Конфигурация сохраняется в `config.yml` и меняется командами.

## Права

- `raiderworld.admin` — полный доступ (по умолчанию OP)
- `raiderworld.start` — запуск рейдов

## Примечания по разработке

Полный AI зомби, ломающих блоки (поиск пути + разрушение с ограничениями по блокам/скорости/дистанции), требует либо Paper API, либо NMS/Custom Goals. В текущей версии это заготовка (теги). Можно расширить в `RaidManager.spawnConfiguredMob` и отдельном `ZombieBreakGoal`.

Проект готов к загрузке на GitHub и дальнейшей доработке.
