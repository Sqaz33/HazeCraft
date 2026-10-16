# Магазин — состояние 0.3.12

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

НЕ реализованы уникальные пассивные procs, активные применения 44 предметов,
imbue, corruption/enhancement, swap/quickbuy/builds/recommended, разблокировка
flex по целям карты, точные native hit volumes/тайминги/particles всех предметов.
Вид магазина использует исходные иконки/шрифты, колонки, tiers и ширины
650/1160 из CSS, но НЕ является точной копией всей Panorama-разметки.
Неподдержанные свойства и специальные эффекты отмечены в карточке.

122 программных теста прошли; управление/визуальный результат в игре
автоматически не проверялись. Предметы ниже — состав исходного каталога,
а не утверждение о готовности их специальных эффектов.

| Предмет | Уровень | Цена | Активный |
|---|---:|---:|---|
| Extended Magazine | 1 | 800 | нет |
| Monster Rounds | 1 | 800 | нет |
| Cultlist Sacrifice | 3 | 3200 | да — применение не перенесено |
| Tesla Bullets | 3 | 3200 | нет |
| Capacitor | 4 | 6400 | да — применение не перенесено |
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
| Heroic Aura | 3 | 3200 | да — применение не перенесено |
| Silence Wave | 3 | 3200 | да — применение не перенесено |
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
| Dispel Magic | 3 | 3200 | да — применение не перенесено |
| Spirit Resilience | 3 | 3200 | нет |
| Bullet Resilience | 3 | 3200 | нет |
| Metal Skin | 3 | 3200 | да — применение не перенесено |
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
| Grit | 1 | 800 | да — применение не перенесено |
| Weapon Shielding | 2 | 1600 | нет |
| Spirit Bubble | 2 | 1600 | нет |
| Battle Vest | 2 | 1600 | нет |
| Enchanter's Emblem | 2 | 1600 | нет |
| Extra Spirit | 1 | 800 | нет |
| Mystic Regeneration | 1 | 800 | нет |
| Improved Spirit | 2 | 1600 | нет |
| Spiritual Overflow | 4 | 6400 | нет |
| Return Fire | 2 | 1600 | да — применение не перенесено |
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
| Slowing Hex | 2 | 1600 | да — применение не перенесено |
| Spirit Sap | 2 | 1600 | да — применение не перенесено |
| Focus Lens | 4 | 6400 | да — применение не перенесено |
| Rusted Barrel | 1 | 800 | да — применение не перенесено |
| Disarming Hex | 3 | 3200 | да — применение не перенесено |
| Rescue Beam | 3 | 3200 | да — применение не перенесено |
| Decay | 3 | 3200 | да — применение не перенесено |
| Scourge | 4 | 6400 | да — применение не перенесено |
| Knockdown | 3 | 3200 | да — применение не перенесено |
| Phantom Strike | 4 | 6400 | да — применение не перенесено |
| Warp Stone | 3 | 3200 | да — применение не перенесено |
| Vortex Web | 4 | 6400 | да — применение не перенесено |
| Refresher | 4 | 6400 | да — применение не перенесено |
| Echo Shard | 4 | 6400 | да — применение не перенесено |
| Torment Pulse | 3 | 3200 | нет |
| Cheat Death | 4 | 6400 | нет |
| Shadow Weave | 3 | 3200 | да — применение не перенесено |
| Majestic Leap | 3 | 3200 | да — применение не перенесено |
| Healing Nova | 3 | 3200 | да — применение не перенесено |
| Restorative Locket | 2 | 1600 | да — применение не перенесено |
| Healing Rite | 1 | 800 | да — применение не перенесено |
| Infuser | 4 | 6400 | да — применение не перенесено |
| Guardian Ward | 2 | 1600 | да — применение не перенесено |
| Divine Barrier | 4 | 6400 | да — применение не перенесено |
| Alchemical Fire | 3 | 3200 | да — применение не перенесено |
| Blood Tribute | 3 | 3200 | да — применение не перенесено |
| Fleetfoot | 2 | 1600 | да — применение не перенесено |
| Kinetic Dash | 2 | 1600 | нет |
| Arcane Surge | 2 | 1600 | нет |
| Unstoppable | 4 | 6400 | да — применение не перенесено |
| Colossus | 4 | 6400 | да — применение не перенесено |
| Cold Front | 2 | 1600 | да — применение не перенесено |
| Arctic Blast | 4 | 6400 | да — применение не перенесено |
| Ethereal Shift | 4 | 6400 | да — применение не перенесено |
| Cursed Relic | 4 | 6400 | да — применение не перенесено |
| Duration Extender | 2 | 1600 | нет |
| Superior Duration | 3 | 3200 | нет |
| Glass Cannon | 4 | 6400 | нет |
| Fury Trance | 3 | 3200 | да — применение не перенесено |
| Vampiric Burst | 4 | 6400 | да — применение не перенесено |
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
| Split Shot | 2 | 1600 | да — применение не перенесено |
| Active Reload | 2 | 1600 | нет |
| Magic Carpet | 4 | 6400 | да — применение не перенесено |
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
| Golden Goose Egg | 1 | 800 | да — применение не перенесено |
