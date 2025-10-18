import telebot
import sqlite3
import json
import random
import time
from datetime import datetime, timedelta

bot = telebot.TeleBot('8351445452:AAHH7svpZKHd50r8tp8m9XRSnAC60HyMAPY')

# Инициализация базы данных
def init_db():
    conn = sqlite3.connect('economy.db')
    c = conn.cursor()

    # Таблица пользователей
    c.execute('''CREATE TABLE IF NOT EXISTS users
                 (user_id INTEGER PRIMARY KEY, 
                  username TEXT,
                  balance INTEGER DEFAULT 100,
                  bank INTEGER DEFAULT 0,
                  daily_claimed INTEGER DEFAULT 0,
                  work_cooldown INTEGER DEFAULT 0,
                  inventory TEXT DEFAULT '{}')''')

    conn.commit()
    conn.close()

# Функции для работы с пользователями
def get_user(user_id):
    conn = sqlite3.connect('economy.db')
    c = conn.cursor()
    c.execute("SELECT * FROM users WHERE user_id = ?", (user_id,))
    user = c.fetchone()
    conn.close()

    if user:
        return {
            'user_id': user[0],
            'username': user[1],
            'balance': user[2],
            'bank': user[3],
            'daily_claimed': user[4],
            'work_cooldown': user[5],
            'inventory': json.loads(user[6])
        }
    return None

def create_user(user_id, username):
    conn = sqlite3.connect('economy.db')
    c = conn.cursor()
    c.execute("INSERT INTO users (user_id, username) VALUES (?, ?)", 
              (user_id, username))
    conn.commit()
    conn.close()

def update_user(user_id, **kwargs):
    conn = sqlite3.connect('economy.db')
    c = conn.cursor()

    if 'inventory' in kwargs:
        kwargs['inventory'] = json.dumps(kwargs['inventory'])

    set_clause = ", ".join([f"{key} = ?" for key in kwargs.keys()])
    values = list(kwargs.values())
    values.append(user_id)

    c.execute(f"UPDATE users SET {set_clause} WHERE user_id = ?", values)
    conn.commit()
    conn.close()

# Команда старт
@bot.message_handler(commands=['start'])
def start_command(message):
    user_id = message.from_user.id
    username = message.from_user.username or message.from_user.first_name

    user = get_user(user_id)
    if not user:
        create_user(user_id, username)
        user = get_user(user_id)

    bot.reply_to(message, 
                 f"👋 Добро пожаловать в экономическую систему, {username}!\n"
                 f"💵 Ваш баланс: {user['balance']} монет\n"
                 f"🏦 В банке: {user['bank']} монет\n\n"
                 f"Доступные команды:\n"
                 f"/balance - проверить баланс\n"
                 f"/work - работать\n"
                 f"/daily - ежедневная награда\n"
                 f"/deposit - положить в банк\n"
                 f"/withdraw - снять с банка\n"
                 f"/shop - магазин\n"
                 f"/inventory - инвентарь")

# Баланс
@bot.message_handler(commands=['balance'])
def balance_command(message):
    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    bot.reply_to(message,
                 f"💼 Баланс {user['username']}:\n"
                 f"💵 Наличные: {user['balance']} монет\n"
                 f"🏦 В банке: {user['bank']} монет\n"
                 f"💰 Общая сумма: {user['balance'] + user['bank']} монет")

# Работа
@bot.message_handler(commands=['work'])
def work_command(message):
    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    current_time = int(time.time())
    if current_time < user['work_cooldown']:
        wait_time = user['work_cooldown'] - current_time
        bot.reply_to(message, f"⏰ Вы устали! Отдохните еще {wait_time} секунд")
        return

    # Случайная зарплата
    salary = random.randint(50, 200)
    new_balance = user['balance'] + salary
    cooldown = current_time + 300  # 5 минут коoldown

    update_user(user_id, balance=new_balance, work_cooldown=cooldown)

    jobs = ["программистом", "дизайнером", "строителем", "водителем", "поваром"]
    job = random.choice(jobs)

    bot.reply_to(message, 
                 f"💼 Вы поработали {job} и заработали {salary} монет!\n"
                 f"💵 Теперь у вас: {new_balance} монет")

# Ежедневная награда
@bot.message_handler(commands=['daily'])
def daily_command(message):
    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    current_time = int(time.time())
    last_claimed = user['daily_claimed']

    # Проверяем, прошло ли 24 часа
    if current_time - last_claimed < 86400 and last_claimed != 0:
        next_claim = last_claimed + 86400
        wait_time = next_claim - current_time
        hours = wait_time // 3600
        minutes = (wait_time % 3600) // 60

        bot.reply_to(message, 
                    f"⏰ Вы уже получали награду сегодня!\n"
                    f"Следующая награда через {hours}ч {minutes}м")
        return

    daily_reward = random.randint(100, 500)
    new_balance = user['balance'] + daily_reward

    update_user(user_id, balance=new_balance, daily_claimed=current_time)

    bot.reply_to(message,
                f"🎁 Ежедневная награда!\n"
                f"💵 Вы получили: {daily_reward} монет\n"
                f"💰 Теперь у вас: {new_balance} монет")

# Банковские операции
@bot.message_handler(commands=['deposit'])
def deposit_command(message):
    try:
        args = message.text.split()
        if len(args) < 2:
            bot.reply_to(message, "❌ Используйте: /deposit <сумма> или /deposit all")
            return

        amount_str = args[1]
        if amount_str.lower() == 'all':
            amount = 'all'
        else:
            amount = int(amount_str)
    except (IndexError, ValueError):
        bot.reply_to(message, "❌ Используйте: /deposit <сумма> или /deposit all")
        return

    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    if amount == 'all':
        amount = user['balance']
    else:
        amount = int(amount)

    if amount <= 0:
        bot.reply_to(message, "❌ Сумма должна быть положительной")
        return

    if amount > user['balance']:
        bot.reply_to(message, "❌ Недостаточно средств")
        return

    new_balance = user['balance'] - amount
    new_bank = user['bank'] + amount

    update_user(user_id, balance=new_balance, bank=new_bank)

    bot.reply_to(message,
                f"🏦 Вы положили в банк {amount} монет\n"
                f"💵 Наличные: {new_balance} монет\n"
                f"🏦 В банке: {new_bank} монет")

@bot.message_handler(commands=['withdraw'])
def withdraw_command(message):
    try:
        args = message.text.split()
        if len(args) < 2:
            bot.reply_to(message, "❌ Используйте: /withdraw <сумма> или /withdraw all")
            return

        amount_str = args[1]
        if amount_str.lower() == 'all':
            amount = 'all'
        else:
            amount = int(amount_str)
    except (IndexError, ValueError):
        bot.reply_to(message, "❌ Используйте: /withdraw <сумма> или /withdraw all")
        return

    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    if amount == 'all':
        amount = user['bank']
    else:
        amount = int(amount)

    if amount <= 0:
        bot.reply_to(message, "❌ Сумма должна быть положительной")
        return

    if amount > user['bank']:
        bot.reply_to(message, "❌ Недостаточно средств в банке")
        return

    new_balance = user['balance'] + amount
    new_bank = user['bank'] - amount

    update_user(user_id, balance=new_balance, bank=new_bank)

    bot.reply_to(message,
                f"🏦 Вы сняли с банка {amount} монет\n"
                f"💵 Наличные: {new_balance} монет\n"
                f"🏦 В банке: {new_bank} монет")

# Магазин
shop_items = {
    "phone": {"name": "📱 Смартфон", "price": 500, "description": "Крутой телефон"},
    "laptop": {"name": "💻 Ноутбук", "price": 1500, "description": "Мощный ноутбук"},
    "car": {"name": "🚗 Машина", "price": 5000, "description": "Быстрая машина"},
    "house": {"name": "🏠 Дом", "price": 20000, "description": "Большой дом"}
}

@bot.message_handler(commands=['shop'])
def shop_command(message):
    shop_text = "🛍️ **Магазин**\n\n"

    for item_id, item in shop_items.items():
        shop_text += f"{item['name']}\n"
        shop_text += f"💵 Цена: {item['price']} монет\n"
        shop_text += f"📝 {item['description']}\n"
        shop_text += f"🛒 Купить: /buy {item_id}\n\n"

    bot.reply_to(message, shop_text)

@bot.message_handler(commands=['buy'])
def buy_command(message):
    try:
        item_id = message.text.split()[1]
    except IndexError:
        bot.reply_to(message, "❌ Используйте: /buy <предмет>")
        return

    if item_id not in shop_items:
        bot.reply_to(message, "❌ Такого предмета нет в магазине")
        return

    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    item = shop_items[item_id]

    if user['balance'] < item['price']:
        bot.reply_to(message, "❌ Недостаточно средств")
        return

    # Добавляем предмет в инвентарь
    inventory = user['inventory']
    if item_id in inventory:
        inventory[item_id] += 1
    else:
        inventory[item_id] = 1

    new_balance = user['balance'] - item['price']

    update_user(user_id, balance=new_balance, inventory=inventory)

    bot.reply_to(message,
                f"🎉 Вы купили {item['name']} за {item['price']} монет!\n"
                f"💵 Осталось: {new_balance} монет")

# Инвентарь
@bot.message_handler(commands=['inventory'])
def inventory_command(message):
    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    inventory = user['inventory']

    if not inventory:
        bot.reply_to(message, "📦 Ваш инвентарь пуст")
        return

    inv_text = "🎒 **Ваш инвентарь:**\n\n"

    for item_id, count in inventory.items():
        if item_id in shop_items:
            item_name = shop_items[item_id]['name']
            inv_text += f"{item_name} - {count} шт.\n"

    bot.reply_to(message, inv_text)

# Передача денег
@bot.message_handler(commands=['pay'])
def pay_command(message):
    try:
        args = message.text.split()
        target_username = args[1].replace('@', '')
        amount = int(args[2])
    except (IndexError, ValueError):
        bot.reply_to(message, "❌ Используйте: /pay @username <сумма>")
        return

    user_id = message.from_user.id
    user = get_user(user_id)

    if not user:
        bot.reply_to(message, "Сначала зарегистрируйтесь с помощью /start")
        return

    if amount <= 0:
        bot.reply_to(message, "❌ Сумма должна быть положительной")
        return

    if amount > user['balance']:
        bot.reply_to(message, "❌ Недостаточно средств")
        return

    # Находим пользователя по username
    conn = sqlite3.connect('economy.db')
    c = conn.cursor()
    c.execute("SELECT * FROM users WHERE username = ?", (target_username,))
    target_user = c.fetchone()
    conn.close()

    if not target_user:
        bot.reply_to(message, "❌ Пользователь не найден")
        return

    if target_user[0] == user_id:
        bot.reply_to(message, "❌ Нельзя переводить себе")
        return

    # Обновляем балансы
    new_balance_user = user['balance'] - amount
    new_balance_target = target_user[2] + amount

    update_user(user_id, balance=new_balance_user)
    update_user(target_user[0], balance=new_balance_target)

    bot.reply_to(message,
                f"💸 Вы перевели {amount} монет пользователю @{target_username}\n"
                f"💵 Ваш баланс: {new_balance_user} монет")


print("INFO: Экономика включена!")
# ВАЖНО: Инициализируем базу данных перед запуском бота
init_db()
bot.infinity_polling()