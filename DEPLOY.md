# Деплой minecraftBack на сервер (HTTPS)

## В чём проблема

Сайт открыт по **HTTPS** (https://night-vision.su/), а API вызывается по **HTTP** (http://212.20.52.103/...). Браузер блокирует такие запросы (Mixed Content). Бэкенд должен быть доступен по **HTTPS**.

---

## 1. Подготовка сервера (один раз)

На сервере (212.20.52.103 или где крутится бэкенд) должны быть:
- Java 21
- Maven (или собираешь jar на своей машине и заливаешь)
- Nginx (обратный прокси + SSL)
- PostgreSQL (если ещё не установлен)

```bash
# Обновление и базовая установка (Ubuntu/Debian)
sudo apt update && sudo apt upgrade -y

# Java 21
sudo apt install -y openjdk-21-jdk

# Maven (опционально, если собираешь на сервере)
sudo apt install -y maven

# Nginx
sudo apt install -y nginx

# Certbot для бесплатного SSL (Let's Encrypt)
sudo apt install -y certbot python3-certbot-nginx
```

---

## 2. Сборка приложения

**Вариант A — на своей машине (Windows):**

```powershell
cd c:\Users\George\PycharmProjects\minecraftBack
.\mvnw.cmd clean package -DskipTests
```

Готовый jar будет: `target\minecraftBack-0.0.1-SNAPSHOT.jar`. Его нужно скопировать на сервер (SCP, WinSCP, rsync).

**Вариант B — на сервере:**

```bash
cd /opt  # или твой каталог
# склонируй репозиторий или залей проект
git clone <твой-репо> minecraftBack
cd minecraftBack
./mvnw clean package -DskipTests
```

---

## 3. Каталог приложения на сервере

```bash
sudo mkdir -p /opt/minecraft-back
sudo useradd -r -s /bin/false mcback  # пользователь для запуска (опционально)
```

Скопируй jar в `/opt/minecraft-back/app.jar` (или оставь имя `minecraftBack-0.0.1-SNAPSHOT.jar`).

Переменные окружения лучше задать в отдельном файле:

```bash
sudo nano /opt/minecraft-back/env
```

Содержимое (подставь свои значения):

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/minecraft
SPRING_DATASOURCE_USERNAME=telegram
SPRING_DATASOURCE_PASSWORD=твой_пароль
JWT_SECRET_KEY=твой_длинный_секрет
REFRESH_SECRET_KEY=другой_длинный_секрет
SUPER_ADMIN_USERNAME=admin
SUPER_ADMIN_PASSWORD=надёжный_пароль
```

```bash
sudo chmod 600 /opt/minecraft-back/env
sudo chown -R mcback:mcback /opt/minecraft-back
```

---

## 4. Systemd — сервис для Spring Boot

```bash
sudo nano /etc/systemd/system/minecraft-back.service
```

Вставь (путь к jar поправь при необходимости):

```ini
[Unit]
Description=Minecraft Backend API
After=network.target postgresql.service

[Service]
Type=simple
User=mcback
WorkingDirectory=/opt/minecraft-back

# переменные из файла
EnvironmentFile=/opt/minecraft-back/env

ExecStart=/usr/bin/java -jar /opt/minecraft-back/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Если jar называется `minecraftBack-0.0.1-SNAPSHOT.jar`, в `ExecStart` укажи полное имя файла.

```bash
sudo systemctl daemon-reload
sudo systemctl enable minecraft-back
sudo systemctl start minecraft-back
sudo systemctl status minecraft-back
```

Приложение должно слушать порт **8080** (по умолчанию из application.properties).

---

## 5. Nginx + HTTPS (обязательно для исправления Mixed Content)

Нужен домен, который указывает на сервер (например `api.night-vision.su` → 212.20.52.103).

**5.1. Поддомен API (рекомендуется)**

Создай A-запись: `api.night-vision.su` → `212.20.52.103`.

На сервере:

```bash
sudo nano /etc/nginx/sites-available/minecraft-api
```

Вставь (замени `api.night-vision.su` на свой домен):

```nginx
server {
    listen 80;
    server_name api.night-vision.su;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/minecraft-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

Получить SSL и переключить сайт на HTTPS:

```bash
sudo certbot --nginx -d api.night-vision.su
```

Certbot сам настроит HTTPS и редирект с HTTP на HTTPS.

**5.2. Если API на том же IP без отдельного домена (только IP)**

Тогда SSL для одного IP без домена через Let's Encrypt нельзя. Варианты:
- Выдать API на поддомен (как выше) — лучше всего.
- Или поставить готовый сертификат на 212.20.52.103 (платный/самоподписанный; самоподписанный браузер будет ругать, но запросы по HTTPS перестанут блокироваться, если пользователь примет сертификат).

Пример для поддомена после certbot — в конфиге будет что-то вроде:

```nginx
server {
    listen 443 ssl;
    server_name api.night-vision.su;
    ssl_certificate /etc/letsencrypt/live/api.night-vision.su/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.night-vision.su/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 6. Что поменять во фронтенде

Вместо базового URL вида:

- `http://212.20.52.103`

нужно использовать HTTPS и, по возможности, поддомен:

- `https://api.night-vision.su`

Где задаётся URL API (переменная окружения, конфиг, константа) — заменить на:

```text
https://api.night-vision.su
```

(без слэша в конце, если в коде к нему дописываются пути вроде `/rank/get`, `/rate/get`, `/cases/get`, `/main-news/get`.)

После этого запросы с https://night-vision.su/ пойдут на https://api.night-vision.su/..., Mixed Content исчезнет.

---

## 7. CORS (если фронт на night-vision.su, API на api.night-vision.su)

В этом проекте уже есть `CorsConfig`. Убедись, что в конфиге разрешён origin твоего фронта, например `https://night-vision.su`. Если домен другой — добавь его в allowed origins.

---

## 8. Краткий чеклист

| Шаг | Действие |
|-----|----------|
| 1 | Домен `api.night-vision.su` → A-запись на IP сервера |
| 2 | Собрать jar: `mvnw clean package -DskipTests` |
| 3 | Скопировать jar на сервер в `/opt/minecraft-back/` |
| 4 | Создать `/opt/minecraft-back/env` с переменными БД и JWT |
| 5 | Поднять сервис systemd `minecraft-back`, проверить `status` |
| 6 | Настроить Nginx для `api.night-vision.su` → `http://127.0.0.1:8080` |
| 7 | Выдать SSL: `certbot --nginx -d api.night-vision.su` |
| 8 | Во фронте заменить базовый URL API на `https://api.night-vision.su` |

После этого ошибки «Mixed Content» и «Failed to fetch» из-за HTTP должны пропасть.
