# Магазин — состояние 0.3.15

Перенос всех 156 предметов ещё не завершён. Наличие предмета в каталоге
не означает подтверждённую native-паритетность всех его эффектов.

Источник предметов: установленная Deadlock клиент 6807 / revision 11154106,
20 октября 2026. Извлечены CRC-проверенные abilities/heroes/generic_data;
состав 156 включённых предметов и их числовые свойства совпали с 6785.
Движение/модель пока сохраняют прежний pin 25950029. Исходные ресурсы приватны.
Лаборатория текущего разбора: D:/HazeCraftLab/reference/6807, вне Git.

## Новое управление и роли

- I: текущие характеристики с сервера, включая предметы, усиления и уровень.
- F8 → Персонаж: точный уровень 1–36; рост HP, урона и силы духа из hero_haze.
- Магазин B, активные предметы Z/X/C/V. Выбор усиливаемого умения после
  покупки; СКМ на купленном предмете позволяет изменить выбор.
- Враждебные мобы (Enemy) — вражеские герои; жители/странствующий торговец
  — союзные герои; остальные мобы — крипы/нейтралы. Свои манекены — враги.
- Жители исключены из стрельбы/атакующих областей и доступны для союзных
  предметов. Крипы доступны как не-геройские цели там, где допускает источник.
- F8 → Персонаж → событие cosmic veil позволяет отдельно проверить Veil
  Walker. Это отладочная подача исходного события; обычные блоки Minecraft
  не объявлены cosmic veil и не включают предмет произвольно.

## Исправленные механики

В 0.3.15 добавлены вылупление Golden Goose Egg и 30 исходных определений
постоянных бонусов (10 типов × 3 временных уровня), сохранение и отображение.

Раздельные пули/дух/мили; Hero/NPC-фильтры; исходные scale-функции; выбранное
умение у Quicksilver/Surge/Echo/Reverb; сохранение выбранного умения;
независимые щиты и сроки; окна накопленного урона; снижение КД Witchmail;
Recharging Rush от исходящего урона; Locket от вражеских способностей и
пропорциональная стамина; лечение Nova/Rescue союзников; Pull при повторном
применении Rescue; Focus Lens; парирование Counterspell; отдельный полёт
Carpet/Leap; проходящие сквозь цели пули Armor Piercer; отражённые пули
Return Fire; условные сопротивления; отрицательное время перезарядки
Holsters; реальные заряды Smoke; Lifestrike от нанесённого урона; размер
Colossus для модели/камеры/коллизий; очистка временных модификаторов.
Сила духа теперь передаётся в фактический размер магазина Haze.

## Что ещё не является достоверным переносом

- Golden Goose Egg: вылупление, расходование и постоянные бонусы перенесены.
  Время активного режима Haze служит временем матча; последовательность RNG
  и все native-условия прерывания канала ещё не подтверждены.
- Buildup: полная native-формула EBuildUpRate, decay, иммунитет и все условия
  срабатывания не восстановлены. Существующее приближение не объявляется 1:1.
- Veil Walker: событие/лечение/невидимость/скорость реализованы, но world
  volumes cosmic veil в обычный Minecraft-мир не импортированы.
- Не перенесены полностью уникальные стаковые правила Spirit Rend/Exposure,
  multishot-конус и бонус за многопопадание Split Shot, все on-hit исключения
  Plated Armor, зарядка Mystic Burst/Shock, native-геометрия волн/пуль предметов.
- Не доказаны native-формулы агрегации всех сопротивлений, регена, vamp,
  NPC-понижений, стаков, slide distance и air move distance. Часть свойств
  не имеет эквивалента у vanilla мобов. Нельзя считать их действие проверенным.
- Panorama-магазин, particles, звуки и анимации предметов не перенесены 1:1.

## Проверки

Полная сборка: 140 тестов прошли, 14 пропущены из-за отсутствующих приватных
записей, ошибок нет. Есть отдельные исполняемые проверки для Nova, Cultist Sacrifice, Torment
Pulse, Recharging Rush, Echo, Witchmail, Locket, Grit/Diviner, Counterspell,
Quicksilver, Lifestrike, Holsters, Extra Charge, Shadow Weave, Veil Walker,
Plated Armor и других предметов. Это проверки правил на программном host,
не тест управления/визуала в запущенном Minecraft и не native-трассы 156 вещей.
Старые нативные повторы движения из удалённой D:/DeadlockHazeLab отмечаются
пропущенными, когда приватной записи нет; их утверждения не заменены новыми.

## Каталог источника

| Предмет | Уровень | Цена | Активный |
|---|---:|---:|---|
| Extended Magazine | 1 | 800 | нет |
| Monster Rounds | 1 | 800 | нет |
| Cultlist Sacrifice | 3 | 3200 | да |
| Tesla Bullets | 3 | 3200 | нет |
| Capacitor | 4 | 6400 | да |
| Hollow Point | 3 | 3200 | нет |
| Opening Rounds | 2 | 1600 | нет |
| High-Velocity Rounds | 1 | 800 | нет |
| Melee Lifesteal | 1 | 800 | нет |
| Rebuttal | 1 | 800 | нет |
| Counterspell | 3 | 3200 | нет |
| Weighted Shots | 3 | 3200 | нет |
| Close Quarters | 1 | 800 | нет |
| Long Range | 2 | 1600 | нет |
| Slowing Bullets | 2 | 1600 | нет |
| Inhibitor | 4 | 6400 | нет |
| Spirit Shredder | 2 | 1600 | нет |
| Heroic Aura | 3 | 3200 | да |
| Silence Wave | 3 | 3200 | да |
| Silencer | 4 | 6400 | нет |
| Berserker | 3 | 3200 | нет |
| Frenzy | 4 | 6400 | нет |
| Siphon Bullets | 4 | 6400 | нет |
| Headshot Booster | 1 | 800 | нет |
| Weakening Headshot | 2 | 1600 | нет |
| Sharpshooter | 3 | 3200 | нет |
| Headhunter | 3 | 3200 | нет |
| Spirit Rend | 3 | 3200 | нет |
| Crippling Headshot | 4 | 6400 | нет |
| Mystic Shot | 2 | 1600 | нет |
| Lucky Shot | 4 | 6400 | нет |
| Point Blank | 3 | 3200 | нет |
| Toxic Bullets | 3 | 3200 | нет |
| Ricochet | 4 | 6400 | нет |
| Extra Health | 1 | 800 | нет |
| Bullet Lifesteal | 2 | 1600 | нет |
| Debuff Reducer | 2 | 1600 | нет |
| Dispel Magic | 3 | 3200 | да |
| Spirit Resilience | 3 | 3200 | нет |
| Bullet Resilience | 3 | 3200 | нет |
| Metal Skin | 3 | 3200 | да |
| Healing Booster | 2 | 1600 | нет |
| Fortitude | 3 | 3200 | нет |
| Leech | 4 | 6400 | нет |
| Sprint Boots | 1 | 800 | нет |
| Trophy Collector | 2 | 1600 | нет |
| Enduring Speed | 2 | 1600 | нет |
| Stamina Mastery | 3 | 3200 | нет |
| Rapid Rounds | 1 | 800 | нет |
| Extra Stamina | 1 | 800 | нет |
| Hunter's Aura | 3 | 3200 | нет |
| Grit | 1 | 800 | да |
| Weapon Shielding | 2 | 1600 | нет |
| Spirit Bubble | 2 | 1600 | нет |
| Battle Vest | 2 | 1600 | нет |
| Enchanter's Emblem | 2 | 1600 | нет |
| Extra Spirit | 1 | 800 | нет |
| Mystic Regeneration | 1 | 800 | нет |
| Improved Spirit | 2 | 1600 | нет |
| Spiritual Overflow | 4 | 6400 | нет |
| Return Fire | 2 | 1600 | да |
| Greater Expansion | 3 | 3200 | нет |
| Mystic Expansion | 1 | 800 | нет |
| Extra Charge | 1 | 800 | нет |
| Spirit Lifesteal | 2 | 1600 | нет |
| Bullet Resist Shredder | 2 | 1600 | нет |
| Mystic Reverb | 4 | 6400 | нет |
| Mystic Burst | 1 | 800 | нет |
| Tankbuster | 3 | 3200 | нет |
| Mystic Vulnerability | 2 | 1600 | нет |
| Healbane | 2 | 1600 | нет |
| Mystic Slow | 2 | 1600 | нет |
| Escalating Exposure | 4 | 6400 | нет |
| Rapid Recharge | 3 | 3200 | нет |
| Compress Cooldown | 2 | 1600 | нет |
| Superior Cooldown | 3 | 3200 | нет |
| Transcendent Cooldown | 4 | 6400 | нет |
| Slowing Hex | 2 | 1600 | да |
| Spirit Sap | 2 | 1600 | да |
| Focus Lens | 4 | 6400 | да |
| Rusted Barrel | 1 | 800 | да |
| Disarming Hex | 3 | 3200 | да |
| Rescue Beam | 3 | 3200 | да |
| Decay | 3 | 3200 | да |
| Scourge | 4 | 6400 | да |
| Knockdown | 3 | 3200 | да |
| Phantom Strike | 4 | 6400 | да |
| Warp Stone | 3 | 3200 | да |
| Vortex Web | 4 | 6400 | да |
| Refresher | 4 | 6400 | да |
| Echo Shard | 4 | 6400 | да |
| Torment Pulse | 3 | 3200 | нет |
| Cheat Death | 4 | 6400 | нет |
| Shadow Weave | 3 | 3200 | да |
| Majestic Leap | 3 | 3200 | да |
| Healing Nova | 3 | 3200 | да |
| Restorative Locket | 2 | 1600 | да |
| Healing Rite | 1 | 800 | да |
| Infuser | 4 | 6400 | да |
| Guardian Ward | 2 | 1600 | да |
| Divine Barrier | 4 | 6400 | да |
| Alchemical Fire | 3 | 3200 | да |
| Blood Tribute | 3 | 3200 | да |
| Fleetfoot | 2 | 1600 | да |
| Kinetic Dash | 2 | 1600 | нет |
| Arcane Surge | 2 | 1600 | нет |
| Unstoppable | 4 | 6400 | да |
| Colossus | 4 | 6400 | да |
| Cold Front | 2 | 1600 | да |
| Arctic Blast | 4 | 6400 | да |
| Ethereal Shift | 4 | 6400 | да |
| Cursed Relic | 4 | 6400 | да |
| Duration Extender | 2 | 1600 | нет |
| Superior Duration | 3 | 3200 | нет |
| Glass Cannon | 4 | 6400 | нет |
| Fury Trance | 3 | 3200 | да |
| Vampiric Burst | 4 | 6400 | да |
| Lifestrike | 3 | 3200 | нет |
| Spirit Strike | 1 | 800 | нет |
| Spirit Snatch | 3 | 3200 | нет |
| Melee Charge | 2 | 1600 | нет |
| Crushing Fists | 4 | 6400 | нет |
| Diviner's Kevlar | 4 | 6400 | нет |
| Radiant Regeneration | 3 | 3200 | нет |
| Boundless Spirit | 4 | 6400 | нет |
| Burst Fire | 3 | 3200 | нет |
| Extra Regen | 1 | 800 | нет |
| Surge of Power | 3 | 3200 | нет |
| Suppressor | 2 | 1600 | нет |
| Quicksilver Reload | 2 | 1600 | нет |
| Mercurial Magnum | 4 | 6400 | нет |
| Intensifying Magazine | 2 | 1600 | нет |
| Escalating Resilience | 3 | 3200 | нет |
| Swift Striker | 2 | 1600 | нет |
| Veil Walker | 3 | 3200 | нет |
| Reactive Barrier | 2 | 1600 | нет |
| Indomitable | 4 | 6400 | нет |
| Restorative Shot | 1 | 800 | нет |
| Titanic Magazine | 2 | 1600 | нет |
| Split Shot | 2 | 1600 | да |
| Active Reload | 2 | 1600 | нет |
| Magic Carpet | 4 | 6400 | да |
| Spirit Burn | 4 | 6400 | нет |
| Lightning Scroll | 4 | 6400 | нет |
| Witchmail | 4 | 6400 | нет |
| Healing Tempo | 4 | 6400 | нет |
| Armor Piercer | 4 | 6400 | нет |
| Plated Armor | 4 | 6400 | нет |
| Spellbreaker | 4 | 6400 | нет |
| Juggernaut | 4 | 6400 | нет |
| Enchanted Holsters | 4 | 6400 | нет |
| Stalker | 2 | 1600 | нет |
| Express Shot | 3 | 3200 | нет |
| Ballistic Enchantment | 3 | 3200 | нет |
| Recharging Rush | 2 | 1600 | нет |
| Golden Goose Egg | 1 | 800 | да |
