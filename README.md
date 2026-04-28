# Promo OTP Service

## Технологии

- Java 17, Maven
- PostgreSQL 17, JDBC (собственный пул соединений без сторонних библиотек)
- `com.sun.net.httpserver` (встроен в JDK)
- JWT (JJWT 0.12.6), BCrypt (jbcrypt)
- Jakarta Mail (angus-mail 2.0.5) — Email
- jSMPP 3.0.1 — SMS через SMPP-эмулятор
- Java 11 HttpClient — Telegram Bot API
- SLF4J + Logback — логирование

## Структура проекта

```
src/main/java/com/promoitotp/
├── App.java                    — точка входа, сборка и запуск сервера
├── api/
│   ├── AuthRouter.java         — POST /auth/register, POST /auth/login
│   ├── AdminRouter.java        — PUT /admin/config, GET /admin/users, DELETE /admin/users/{id}
│   ├── OtpRouter.java          — POST /otp/generate, POST /otp/validate
│   ├── AccessFilter.java       — JWT-фильтр проверки роли
│   ├── RequestLogger.java      — логирование каждого запроса
│   └── HttpResponse.java       — утилиты для отправки HTTP-ответов
├── core/
│   ├── UserService.java        — регистрация, аутентификация, управление пользователями
│   ├── OtpService.java         — генерация и валидация OTP
│   └── ExpiredOtpCleaner.java  — фоновый поток: помечает просроченные OTP
├── dao/
│   ├── ConnectionPool.java     — пул JDBC-соединений на BlockingQueue
│   ├── UserDao.java            — запросы к таблице users
│   ├── OtpDao.java             — запросы к таблице otp_codes
│   └── OtpSettingsDao.java     — запросы к таблице otp_config
├── model/
│   ├── UserRecord.java         — модель пользователя
│   ├── OtpEntry.java           — модель OTP-кода
│   ├── OtpSettings.java        — модель конфигурации OTP
│   └── OtpStatus.java          — enum: ACTIVE, EXPIRED, USED
├── notify/
│   ├── Sender.java             — интерфейс канала рассылки
│   ├── EmailSender.java        — отправка через SMTP
│   ├── SmsSender.java          — отправка через SMPP
│   ├── TelegramSender.java     — отправка через Telegram Bot API
│   └── FileSender.java         — сохранение в файл
└── util/
    ├── Props.java              — загрузка .properties с поддержкой env-переменных
    ├── Json.java               — сериализация/десериализация JSON (Jackson)
    └── Tokens.java             — выдача и верификация JWT
```

## База данных

3 таблицы, создаются автоматически при первом запуске:

- **users** — логин, хеш пароля (BCrypt), роль (`ADMIN` / `USER`)
- **otp_config** — единственная строка: длина кода (4–10 цифр) и TTL в секундах
- **otp_codes** — коды со статусами `ACTIVE` / `EXPIRED` / `USED`, привязанные к пользователю и операции

## Быстрый старт

```bash
# 1. Создать базу данных
psql -U postgres -c "CREATE DATABASE otpdb;"

# 2. Собрать fat JAR
mvn clean package

# 3. Запустить
java -jar target/promo-otp-service-1.0.0.jar
```

Сервер запускается на порту **8080**.

## Конфигурация

### `src/main/resources/app.properties`
```properties
server.port=8080
server.threads=10

db.url=jdbc:postgresql://localhost:5432/otpdb
db.user=postgres
db.pass=postgres

jwt.secret=promo-it-secret-key-must-be-32-chars-min!!
jwt.ttl.seconds=3600

otp.file.dir=otp_files
```

Любое свойство можно переопределить через переменную окружения:  
`db.url` → `DB_URL`, `jwt.secret` → `JWT_SECRET` и т.д.

### `src/main/resources/email.properties`
```properties
smtp.host=localhost
smtp.port=1025
smtp.auth=false
smtp.from=otp@promoitservice.local
```

Для реальной почты (Gmail):
```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.auth=true
smtp.starttls=true
smtp.user=your@gmail.com
smtp.pass=your-app-password
smtp.from=your@gmail.com
```

Запуск эмулятора MailHog:
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
# Web UI: http://localhost:8025
```

### `src/main/resources/sms.properties`
```properties
smpp.host=localhost
smpp.port=2775
smpp.login=smppclient1
smpp.pass=password
smpp.sender=PromoOTP
```

Запуск SMPPsim: скачать и выполнить `startsmppsim.bat` (порт 2775).

### `src/main/resources/telegram.properties`
```properties
tg.token=YOUR_BOT_TOKEN_HERE
tg.chat=YOUR_CHAT_ID_HERE
```

Как получить `chat_id`:
1. Создать бота через @BotFather → получить токен
2. Написать боту любое сообщение
3. Открыть: `https://api.telegram.org/bot<TOKEN>/getUpdates`
4. Найти `"chat": { "id": ... }` в ответе

## API

### POST /auth/register
Регистрация пользователя. Только один ADMIN может существовать.

```json
{ "login": "ivan", "password": "secret123", "role": "USER" }
```

Ответы: `201` — создан, `400` — ошибка валидации, `409` — логин занят или admin уже есть.

---

### POST /auth/login
```json
{ "login": "ivan", "password": "secret123" }
```

Ответ `200`:
```json
{ "token": "<JWT>" }
```

Токен используется во всех защищённых запросах: `Authorization: Bearer <token>`

Ответ `401` — неверный логин/пароль.

---

### PUT /admin/config *(роль: ADMIN)*
Изменить настройки OTP-кодов.

```json
{ "codeLength": 6, "ttlSeconds": 300 }
```

Ответ `200`:
```json
{ "codeLength": 6, "ttlSeconds": 300 }
```

---

### GET /admin/users *(роль: ADMIN)*
Список пользователей с ролью USER.

Ответ `200`:
```json
[{ "id": 1, "login": "ivan", "role": "USER", "createdAt": "..." }]
```

---

### DELETE /admin/users/{id} *(роль: ADMIN)*
Удалить пользователя и все его OTP-коды (CASCADE).

Ответы: `204` — удалён, `404` — не найден или является администратором.

---

### POST /otp/generate *(роль: USER)*
Сгенерировать OTP и отправить через выбранный канал.

```json
{
  "operationId": "payment-001",
  "channel": "EMAIL",
  "destination": "ivan@example.com"
}
```

`channel`: `EMAIL` | `SMS` | `TELEGRAM` | `FILE`

`destination`: адрес email (EMAIL), номер телефона (SMS). Для `TELEGRAM` и `FILE` можно передать пустую строку.

Ответ `200`:
```json
{ "message": "OTP успешно отправлен" }
```

---

### POST /otp/validate *(роль: USER)*
Проверить OTP-код.

```json
{ "operationId": "payment-001", "code": "482910" }
```

Ответ `200` (верный):
```json
{ "valid": true, "message": "Код подтверждён" }
```

Ответ `400` (неверный или истёкший):
```json
{ "valid": false, "message": "Код неверен или истёк" }
```

## Логирование

Каждый HTTP-запрос логируется в формате:
```
METHOD /path STATUS | login | ip=x.x.x.x | Nms
```
- `4xx` → уровень `WARN`
- `5xx` → уровень `ERROR`
- остальные → `INFO`

Логи пишутся в консоль и в файл `logs/app.log` с ежедневной ротацией (хранение 14 дней).

## Механизм просрочки OTP

`ExpiredOtpCleaner` каждые **60 секунд** выполняет запрос:
```sql
UPDATE otp_codes SET status = 'EXPIRED'
WHERE status = 'ACTIVE' AND expires_at < NOW()
```
