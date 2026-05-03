# Eldes Alice Skill

Ktor-сервис для управления шлагбаумами двора и воротами паркинга через Яндекс Алису.

Основной режим - интеграция с Yandex Smart Home API. В этом режиме ворота появляются в приложении "Дом с Алисой" как устройства, и их можно открывать без запуска обычного навыка.

Старый webhook обычного навыка Алисы (`POST /alice`) оставлен для обратной совместимости.

## Возможности

Навык управляет четырьмя направлениями:

- въезд во двор;
- выезд из двора;
- въезд в паркинг;
- выезд из паркинга.

Smart Home endpoints:

- `HEAD /v1.0/` - проверка endpoint URL провайдера.
- `GET /v1.0/user/devices` - список устройств пользователя.
- `POST /v1.0/user/devices/query` - состояние устройств.
- `POST /v1.0/user/devices/action` - открытие устройства.
- `POST /v1.0/user/unlink` - отвязка аккаунта.

OAuth endpoints для привязки аккаунта:

- `GET /oauth/authorize`
- `POST /oauth/authorize`
- `POST /oauth/token`

Healthcheck сервиса: `GET /ping`.

Webhook обычного навыка: `POST /` или `POST /alice`.

## Команды Алисы

В Smart Home режиме после привязки аккаунта можно говорить без запуска навыка:

- `Алиса, открой шлагбаум` - въезд во двор.
- `Алиса, открой шлагбаум выезд` - выезд из двора.
- `Алиса, открой ворота паркинга въезд` - въезд в паркинг.
- `Алиса, открой ворота паркинга выезд` - выезд из паркинга.

Команды обычного навыка:

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

smartHome:
  oauthClientId: "$SMART_HOME_OAUTH_CLIENT_ID:"
  oauthClientSecret: "$SMART_HOME_OAUTH_CLIENT_SECRET:"
  authCodeTtlSeconds: "$SMART_HOME_AUTH_CODE_TTL_SECONDS:300"
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
- `SMART_HOME_OAUTH_CLIENT_ID` - client id из настроек OAuth в Яндекс Диалогах.
- `SMART_HOME_OAUTH_CLIENT_SECRET` - client secret из настроек OAuth в Яндекс Диалогах.
- `SMART_HOME_AUTH_CODE_TTL_SECONDS` - срок жизни OAuth authorization code.

## Настройка в Яндекс Диалогах

Создайте диалог типа "Умный дом".

Название:

```text
Шлагбаумы на Роще
```

Подзаголовок:

```text
Управление шлагбаумами и воротами паркинга
```

Backend:

```text
https://sim-sim.housekpr.ru
```

Яндекс сам добавляет Smart Home paths вроде `/v1.0/user/devices`, поэтому в поле Backend нужно указывать базовый URL без `/v1.0`.

OAuth URL авторизации:

```text
https://sim-sim.housekpr.ru/oauth/authorize
```

OAuth token URL:

```text
https://sim-sim.housekpr.ru/oauth/token
```

При привязке аккаунта житель вводит существующие email и пароль от Eldes/Gate. Backend проверяет этого пользователя и отдает Яндексу только доступные ему устройства.

### Текст для формы Яндекс Диалогов

Подключение устройств:

```text
1. Убедитесь, что у вас есть учетная запись в сервисе доступа к шлагбаумам и воротам дома на Роще.
2. Войдите в приложение Дом с Алисой.
3. В приложении Дом с Алисой нажмите кнопку "+".
4. Выберите "Устройство умного дома".
5. В списке производителей выберите "Шлагбаумы на Роще".
6. Нажмите "Объединить аккаунты".
7. Авторизуйтесь, используя email и пароль от сервиса доступа к шлагбаумам.
8. После успешной привязки аккаунта в Доме с Алисой появятся доступные вам устройства: шлагбаум двора и ворота паркинга.
```

Список поддерживаемых устройств:

```text
Поддерживаются устройства управления доступом:

1. Шлагбаум — въезд во двор.
2. Шлагбаум выезд — выезд из двора.
3. Ворота паркинга въезд — въезд в паркинг.
4. Ворота паркинга выезд — выезд из паркинга.

Тип устройств: открываемые устройства, ворота и шлагбаумы.
Поддерживаемое действие: открыть.
Состояние открытия/закрытия не отображается, устройство выполняет импульсную команду открытия.
```

## Запуск

```bash
export ELDES_API_BASE_URL=http://localhost:8080
export ELDES_API_EMAIL=user@example.com
export ELDES_API_PASSWORD=password
export SMART_HOME_OAUTH_CLIENT_ID=client-id-from-yandex
export SMART_HOME_OAUTH_CLIENT_SECRET=client-secret-from-yandex

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

Проверка Smart Home endpoint:

```bash
curl -I http://localhost:8081/v1.0/
```

Проверка OAuth-формы:

```bash
curl -i 'http://localhost:8081/oauth/authorize?client_id=test&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback&state=123'
```

Docker-сборка:

```bash
docker build -t eldes-alice .
docker run --rm -p 8081:8081 \
  -e ELDES_API_BASE_URL=http://host.docker.internal:8080 \
  -e ELDES_API_EMAIL=user@example.com \
  -e ELDES_API_PASSWORD=password \
  -e SMART_HOME_OAUTH_CLIENT_ID=client-id-from-yandex \
  -e SMART_HOME_OAUTH_CLIENT_SECRET=client-secret-from-yandex \
  eldes-alice
```
