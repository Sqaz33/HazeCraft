# Магазин — состояние 0.3.13

Это предварительная сборка, не перенос всех предметов 1:1.

Источник: установленная Deadlock build 25950029; текущие abilities.vdata,
generic_data.vdata, Panorama XML/CSS, 156 оригинальных иконок и английская
локализация. Отключённые m_bDisabled и шаблоны _not_pickable исключены.

Работают каталог 156 включённых предметов, B для открытия, поиск, прокрутка,
ЛКМ для покупки/улучшения, ПКМ для продажи, бесконечные души, 8 начальных
слотов и 4 закрытых, ограничение 4 активных слотов, замена компонентов без
двойного суммирования, цены с вычетом компонентов, сохранение в папке мира
haze-items/UUID.json, значки предметов на HUD.

Подключены основные плоские характеристики: HP/реген/реген вне боя, урон
оружия/мобам, темп стрельбы, магазин, скорость пули, перезарядка, дальность
падения урона, сила духа для существующих scale-функций, часть длительности/
КД/дальности умений, вампиризм пуль, урон мили, скорость/спринт/стамина/реген
стамины, сопротивление пулям и мили. Данные характеристик берутся только из
авторегистрируемых исходных m_AutoIntrinsicModifiers и m_mapAbilityProperties.
Формулы всех разновидностей агрегирования native stats ещё не подтверждены.

В 0.3.13 добавлен серверный ItemEffects: активные слоты Z/X/C/V, исходные
КД и задержки применения, щиты и их расход/истечение, лечение во времени,
телепорты, вертикальный импульс, полёт, невидимость, очистка, отражение,
периодический урон, замедления и блокировка атак, срабатывания от пуль,
хедшотов, мили, умений и входящего урона. Добавлены обработчики цепной
молнии, рикошета, критов, лечения от выстрела, Spirit Strike/Lifestrike,
Mystic Burst/Shock/Reverb, Burst Fire, Berserker, условных усилений,
накопления брони, Quicksilver Reload, активной перезарядки и восстановления
зарядов. HP-проценты Colossus/Glass Cannon исправлены.

Это всё ещё НЕ полная копия всех 156 предметов. Не завершены imbue/выбор
улучшаемого умения, Veil Walker (в Minecraft нет исходных veil volumes),
точная механика Counterspell, Armor Piercer, разделение hero/NPC во всех
формулах, native buildup decay/пороговые окна и все специальные условия.
Ряд активных предметов адаптирован приближённо: геометрия волн/цепей,
канал Rescue Beam, поведение Scourge, старт/прерывание полёта. Некоторые
статы движения пока не подключены. Не подтверждено точное native
агрегирование сопротивлений. Частицы/звук/анимации предметов и весь
Panorama-магазин 1:1 не перенесены. Наличие обработчика и прохождение
входа всеми 44 активными не означают полной достоверности их поведения.

Во время работы пользователь отменил любые дальнейшие проверки/тесты.
Последняя версия собрана с исключением test; игровой прогон не выполнялся.
Прежний тест начала Healing Nova ожидал лечение до завершения исходной
задержки .25 секунды; ожидание исправлено, повторный запуск не выполнялся.

Ниже — состав исходного каталога, а не гарантия готовности всех эффектов.

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
