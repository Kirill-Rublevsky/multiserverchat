# OVENQUILL GlobalChat

Межсерверный глобальный чат для Velocity 4.3.0 и Purpur. Локальный чат остаётся под полным управлением ChatViramble; только сообщения, начинающиеся ровно с `!`, перехватываются backend-модулем и доставляются всем игрокам прокси.

## Почему два JAR

Velocity официально предупреждает: отмена подписанного сообщения Minecraft 1.19.1+ в `PlayerChatEvent` может отключить игрока. Поэтому proxy-only реализация не может одновременно гарантировать отсутствие кика и локального дубля. Минимальный Purpur-модуль безопасно отменяет глобальное сообщение через Paper `AsyncChatEvent`, не касается обычного чата и отправляет оригинальный текст на Velocity. Velocity проверяет HMAC-SHA256, срок пакета, nonce, UUID игрока и текущий backend.

## Установка

1. Положите `ovenquill-globalchat-velocity-1.0.0.jar` в `plugins/` Velocity.
2. Положите `ovenquill-globalchat-backend-1.0.0.jar` в `plugins/` **каждого** Purpur-сервера.
3. Один раз запустите прокси и backend-серверы, затем остановите их.
4. Сгенерируйте длинную случайную строку (не менее 16 символов) и укажите одинаковое значение:
   - Velocity: `plugins/ovenquill-globalchat/config.yml` → `backend.shared-secret`;
   - каждый Purpur: `plugins/OvenquillGlobalChatBackend/config.yml` → `shared-secret`.
5. Проверьте одинаковый `trigger` (по умолчанию `!`) и перезапустите сеть.

Без настроенного секрета модули намеренно блокируют глобальные сообщения с понятной диагностикой. Обычный локальный чат продолжает работать.

## Команды и permissions

- `/globalchat help` (`/gc`) — справка;
- `/globalchat reload` — `ovenquill.globalchat.reload`;
- `/globalchat status` — `ovenquill.globalchat.admin`;
- `/globalchat toggle` — `ovenquill.globalchat.admin`, состояние сохраняется в YAML;
- `ovenquill.globalchat.bypasscooldown` — обход задержки (защита от повторов сохраняется).

Обычным игрокам permission для глобального чата не нужен.

## Конфигурация

Все реально используемые параметры снабжены понятными именами в `velocity/src/main/resources/config.yml`. Текстовые ответы находятся в `messages.yml`. Формат поддерживает MiniMessage и `{prefix}`, `{player}`, `{message}`, `{server}`. Значения игроков передаются через `Placeholder.unparsed`, поэтому MiniMessage-теги в нике и сообщении не исполняются. Длина считается по Unicode code points.

## Сборка

Требуется JDK 25+ и Gradle 9: Velocity API 4.3.0 сам имеет минимальный ABI Java 25. Backend/common компилируются в Java 21, Velocity — в Java 25. Выполните `gradle clean build`. Готовые файлы появятся в `velocity/build/libs/` и `backend/build/libs/`.

## Проверки

| Проверка | Результат |
|---|---|
| Локальное сообщение / пробел перед `!` | backend listener возвращается без изменений; ChatViramble получает событие |
| `!Привет`, `! Привет`, `!!Привет` | удаляется один trigger на Velocity; пробелы по краям удаляются |
| `!` / `!   ` | отказ `empty-message` |
| Межсерверная доставка и отправитель | одна рассылка по `ProxyServer#getAllPlayers` |
| Signed chat / отсутствие local duplicate | отмена только на Paper, Velocity `PlayerChatEvent` не отменяется и не изменяется |
| Cooldown / duplicate / Unicode length | атомарное состояние `ConcurrentHashMap`; code-point подсчёт |
| MiniMessage injection | пользовательские данные вставляются как `Placeholder.unparsed` |
| Подмена / replay backend-пакета | HMAC-SHA256, max age, одноразовый nonce, проверка player/server |
| Ошибка одного получателя | исключение логируется, рассылка продолжается |
| Toggle / restart | значение сохраняется в `config.yml` |

Автоматические unit-тесты покрывают протокольный round-trip, HMAC, срок пакета, Unicode и правила trigger. Полный интеграционный прогон требует реальной тестовой сети Velocity + Purpur + ChatViramble и описан в [TESTING.md](TESTING.md).

## Известные ограничения

- Backend JAR обязателен на каждом Purpur-сервере: это следствие signed-chat протокола, а не дополнительная функция.
- Статус backend в команде показывает число зарегистрированных серверов Velocity; активная проверка heartbeat не используется, чтобы не создавать лишний трафик. Ошибочная/отсутствующая установка выявляется диагностикой на конкретном backend.
- Версия Paper API в сборке задаёт минимальную ABI-цель; Purpur целевой версии Minecraft должен предоставлять совместимый Paper API.
