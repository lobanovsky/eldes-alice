# Eldes Alice Skill

Ktor webhook для навыка Яндекс Алисы, который открывает шлагбаумы двора и ворота паркинга через существующий Eldes API.

## Возможности

Навык управляет четырьмя направлениями:

- въезд во двор;
- выезд из двора;
- въезд в паркинг;
- выезд из паркинга.

Webhook для Яндекс Диалогов принимает запросы на `POST /` и `POST /alice`.
Проверка работоспособности: `GET /ping`.

## Команды Алисы

Въезд во двор:

- `сим-сим откройся`
- `открой шлагбаум на въезд`

Выезд из двора:

- `сим-сим в путь`
- `открой шлагбаум на выезд`

Въезд в паркинг:

- `сим-сим вниз`
- `открой ворота паркинга на въезд`

Выезд из паркинга:

- `сим-сим вверх`
- `открой ворота паркинга на выезд`

## Настройки

Основные настройки находятся в `src/main/resources/application.yaml`.
Значения можно переопределять через переменные окружения:

```yaml
eldesApi:
  baseUrl: "$ELDES_API_BASE_URL:http://localhost:8080"
  email: "$ELDES_API_EMAIL:email@eldes.com"
  password: "$ELDES_API_PASSWORD:password"
  timeoutMs: "$ELDES_API_TIMEOUT_MS:4000"
  connectTimeoutMs: "$ELDES_API_CONNECT_TIMEOUT_MS:1500"
  socketTimeoutMs: "$ELDES_API_SOCKET_TIMEOUT_MS:4000"
```

Переменные:

- `ELDES_API_BASE_URL` - базовый URL Eldes API, без `/api`.
- `ELDES_API_EMAIL` - email пользователя Eldes API.
- `ELDES_API_PASSWORD` - пароль пользователя Eldes API.
- `ELDES_API_TIMEOUT_MS` - общий timeout запроса к Eldes API.
- `ELDES_API_CONNECT_TIMEOUT_MS` - timeout подключения к Eldes API.
- `ELDES_API_SOCKET_TIMEOUT_MS` - socket timeout клиента Eldes API.
- `PORT` - порт Ktor-сервера, по умолчанию `8081`.
- `LOG_LEVEL` - уровень логирования, по умолчанию `INFO`.

## Запуск

```bash
export ELDES_API_BASE_URL=http://localhost:8080
export ELDES_API_EMAIL=user@example.com
export ELDES_API_PASSWORD=password

./gradlew run
```

После запуска:

```bash
curl http://localhost:8081/ping
```

Ожидаемый ответ:

```text
pong
```

## Логирование

Логи пишутся в консоль и в файл:

```text
logs/eldes-alice.log
```

Файловые логи ротируются по дате и размеру:

- шаблон архива: `logs/eldes-alice.YYYY-MM-DD.N.log.gz`;
- срок хранения: 30 дней;
- максимум одного файла: 5 MB;
- общий лимит: 100 MB.

## Проверка

```bash
./gradlew test
```

Docker-сборка:

```bash
docker build -t eldes-alice .
docker run --rm -p 8081:8081 \
  -e ELDES_API_BASE_URL=http://host.docker.internal:8080 \
  -e ELDES_API_EMAIL=user@example.com \
  -e ELDES_API_PASSWORD=password \
  eldes-alice
```
