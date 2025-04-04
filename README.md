# 🔑 rPass

[![Version](https://img.shields.io/badge/version-1.0-brightgreen.svg)](https://github.com/therayzv/rPass/releases)
[![API](https://img.shields.io/badge/API-1.21-blue.svg)](https://www.spigotmc.org/)

> Простой и мощный плагин для управления проходками (пропусками) на вашем сервере Minecraft

![rPass Preview](https://via.placeholder.com/800x400?text=rPass+Plugin+Preview)

## ✨ Возможности

- **Управление проходками**: выдача, продление и отзыв проходок для игроков
- **Разные типы проходок**: временные (на конкретный срок) и постоянные
- **Гибкие форматы времени**: поддержка различных форматов (дни, часы, минуты)
- **Удобное хранение**: сохранение проходок по никнеймам для легкого редактирования
- **Интеграция с PlaceholderAPI**: богатый набор плейсхолдеров для использования
- **Асинхронная обработка**: быстрая работа даже при большом количестве игроков
- **Кастомизация сообщений**: полная настройка всех сообщений плагина

## 📋 Команды

| Команда | Описание | Права |
|---------|----------|-------|
| `/rpass give <игрок> <время>` | Выдать или продлить проходку | `rpass.give` |
| `/rpass take <игрок>` | Забрать проходку | `rpass.take` |
| `/rpass extendall <время>` | Продлить проходку всем игрокам | `rpass.extendall` |
| `/rpass info [игрок]` | Информация о проходке | `rpass.info` |
| `/rpass list` | Список всех проходок | `rpass.list` |
| `/rpass reload` | Перезагрузить конфиг | `rpass.reload` |
| `/rpass debug` | Отладочная информация | `rpass.debug` |

### Форматы времени
- `30m` - 30 минут
- `12h` - 12 часов
- `7d` - 7 дней
- `permanent` или `навсегда` - бессрочная проходка

## 🔄 Плейсхолдеры

Для использования плейсхолдеров требуется [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI).

| Плейсхолдер | Описание |
|-------------|----------|
| `%rpass_has%` | Имеет ли игрок проходку (true/false) |
| `%rpass_time%` | Время до окончания проходки с правильным склонением |
| `%rpass_time_compact%` | Компактное отображение времени (7д 12ч 30м) |
| `%rpass_days%` | Количество дней до окончания проходки |
| `%rpass_hours%` | Количество часов до окончания проходки |
| `%rpass_minutes%` | Количество минут до окончания проходки |
| `%rpass_seconds%` | Количество секунд до окончания проходки |
| `%rpass_permanent%` | Является ли проходка вечной (true/false) |
| `%rpass_status%` | Статус проходки (Активна/Истекла/Нет проходки) |

## ⚙️ Конфигурация

Плагин предлагает расширенные возможности настройки через файл `config.yml`:

- **Настройка сообщений**: все сообщения плагина можно кастомизировать
- **Настройка кика**: сообщения при истечении проходки и попытке входа без неё
- **Настройка плейсхолдеров**: тексты для различных статусов проходки
- **Интервал проверки**: частота проверки наличия действующих проходок

## 🛠️ Установка

1. Скачайте последнюю версию плагина из [releases](https://github.com/therayzv/rPass/releases)
2. Поместите файл `.jar` в папку `plugins` вашего сервера
3. Перезапустите сервер или используйте плагин для загрузки плагинов
4. Настройте `config.yml` по вашему желанию
5. Если используете PlaceholderAPI, выполните команду `/papi reload` после запуска плагина

## 📄 Права

| Право | Описание |
|-------|----------|
| `rpass.admin` | Доступ ко всем командам плагина |
| `rpass.give` | Доступ к выдаче и продлению проходок |
| `rpass.take` | Доступ к удалению проходок |
| `rpass.extendall` | Доступ к продлению проходок всем игрокам |
| `rpass.info` | Доступ к информации о проходках |
| `rpass.list` | Доступ к просмотру списка проходок |
| `rpass.reload` | Доступ к перезагрузке плагина |
| `rpass.debug` | Доступ к отладочной информации |
