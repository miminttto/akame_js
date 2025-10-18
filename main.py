import telebot
from telebot import types
from telebot.types import ChatPermissions
import datetime
from datetime import timedelta
import random
import json
import os
import logging

bot = telebot.TeleBot('8351445452:AAHH7svpZKHd50r8tp8m9XRSnAC60HyMAPY')

# Загрузка данных гендера из файла

GENDER_DATA_FILE = 'gender_data.json'

muted_users = {}

def load_gender_data():
    if os.path.exists(GENDER_DATA_FILE):
        with open(GENDER_DATA_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {}

def save_gender_data(data):
    with open(GENDER_DATA_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

gender_data = load_gender_data()

# Логер
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('bot.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger('AkameBot')



@bot.message_handler(commands=['start'])
def start(message):

    glav = types.InlineKeyboardMarkup()
    help = types.InlineKeyboardButton(text='🎀 Помощь', callback_data='help2')
    cmd = types.InlineKeyboardButton(text='🎯 Команды', callback_data='cmd2')
    info = types.InlineKeyboardButton(text='ℹ️ Информация', callback_data='info2')
    glav.add(help, cmd, info)

    @bot.callback_query_handler(func=lambda call: True)
    def callback_handler(call):
        if call.data == 'help2':
            bot.send_message(call.message.chat.id, '''
╔══════════════════╗
║              🎀 ПОМОЩЬ            ║
╚══════════════════╝

🎯 𝐎𝐂𝐍𝐎𝐁𝐍𝐎𝐄
├─ ❓ a:help » помощь
├─ ⚙️ a:cmd » команды
├─ ℹ️ a:info » информация
├─ ⚧ /setgender » пол
├─ 👤 /mygender » мой пол
└─ 📊 /genderstats » статистика

💞 𝐑𝐏-𝐊𝐎𝐌𝐀𝐍𝐃𝐘
├─ 💋 a:kiss [user] » поцеловать
├─ 🤗 a:hug [user] » обнять
├─ 🐱 a:myr [user] » мурчать
├─ 😴 a:sleep [user] » спать
└─ 🥰 a:pat [user] » погладить

💡 𝐏𝐑𝐈𝐌𝐄𝐂𝐇𝐀𝐍𝐈𝐄
✨ Префикс *akame:* = *a:*
    Пример: *akame:hug [user]*

✦ ─── ⋆⋅☆⋅⋆ ─── ✦
''')
            logger.info(f'Кнопка: Помощь | Пользователь: {call.from_user.username}')

        elif call.data == 'cmd2':
            bot.send_message(call.message.chat.id, '''
╔══════════════════╗
║           🎯 КОМАНДЫ             ║
╚══════════════════╝

🎮 𝐎𝐒𝐍𝐎𝐕𝐍𝐘𝐄
├─ ❓ a:help » помощь
├─ ⚙️ a:cmd » команды
└─ ℹ️ a:info » информация

💞 𝐑𝐏-𝐊𝐎𝐌𝐀𝐍𝐃𝐘  
├─ 💋 a:kiss » поцеловать
├─ 🤗 a:hug » обнять
├─ 🐱 a:myr » помурчать
├─ 😴 a:sleep » спать
└─ 🥰 a:pat » погладить

👤 𝐆𝐄𝐍𝐃𝐄𝐑
├─ ⚧ /setgender » установить пол
├─ 👤 /mygender » мой пол
└─ 📊 /genderstats » статистика

✦ ─── ⋆⋅☆⋅⋆ ─── ✦
''')
            logger.info(f'Кнопка: Команды | Пользователь: {call.from_user.username}')

        elif call.data == 'info2':
            bot.send_message(call.message.chat.id, '''
╔═══════════════════════╗
║                  👨‍💻 Персонал                       ║
╚═══════════════════════╝
├─ 🔥Создатель: @treplebeska
├─💄Бета-тестер:
└─🤚Хелпер: @Broiashka9
''')


    bot.send_message(message.chat.id, '''
Привет я Акаме.
Я создана чтобы добавить развлечения в телеграмм группу или сервер развлечения!

(дᴀнный боᴛ нᴀходиᴛьᴄя ʙ ᴩᴀзᴩᴀбоᴛᴋᴇ, ᴨо϶ᴛоʍу ʍоᴦуᴛ быᴛь бᴀᴦи и чᴀᴄᴛо ʙыᴋᴧючᴇн.)
''', reply_markup=glav)
    logger.info(f'Команда: start | Пользователь: {message.from_user.username}')

# Функция для получения пола пользователя

@bot.message_handler(commands=['setgender', 'gender'])
def set_gender(message):
    chat_id = message.chat.id
    user_id = message.from_user.id

    markup = types.InlineKeyboardMarkup()
    male_btn = types.InlineKeyboardButton('♂️ Мужской', callback_data=f'gender_male_{chat_id}')
    female_btn = types.InlineKeyboardButton('♀️ Женский', callback_data=f'gender_female_{chat_id}')
    markup.add(male_btn, female_btn)

    bot.send_message(chat_id, 
                    f"👤 {message.from_user.first_name}, выбери свой пол:",
                    reply_markup=markup)
    logger.info(f'Команда: setgender | Пользователь: {message.from_user.username}')

@bot.callback_query_handler(func=lambda call: call.data.startswith('gender_'))
def gender_callback(call):
    user_id = call.from_user.id
    chat_id = int(call.data.split('_')[-1])
    gender = call.data.split('_')[1]

    if str(chat_id) not in gender_data:
        gender_data[str(chat_id)] = {}

    # Сохраняем пол пользователя
    gender_data[str(chat_id)][str(user_id)] = gender
    save_gender_data(gender_data)

    gender_text = "♂️ мужской" if gender == "male" else "♀️ женский"
    bot.answer_callback_query(call.id, f"✅ Твой пол установлен: {gender_text}")
    bot.edit_message_text(f"✅ Твой пол установлен: {gender_text}", 
                         call.message.chat.id, call.message.message_id)
    logger.info(f'Команда: setgender | Пользователь: {call.from_user.username} | Пол: {gender_text}')

# Команда для просмотра своего пола
@bot.message_handler(commands=['mygender'])
def my_gender(message):
    chat_id = message.chat.id
    user_id = message.from_user.id

    gender = get_user_gender(chat_id, user_id)
    if gender:
        gender_text = "♂️ мужской" if gender == "male" else "♀️ женский"
        bot.reply_to(message, f"👤 Твой пол: {gender_text}")
        logger.info(f'Команда: mygender | Пользователь: {message.from_user.username} | Пол: {gender_text}')
    else:
        bot.reply_to(message, "❌ Ты еще не установил(а) свой пол. Используй /setgender")
        logger.info(f'Команда: mygender | Пользователь: {message.from_user.username} | Ошибка: Пол не установлен')

# Команда для просмотра статистики по чату
@bot.message_handler(commands=['genderstats'])
def gender_stats(message):
    chat_id = message.chat.id

    if str(chat_id) not in gender_data or not gender_data[str(chat_id)]:
        bot.reply_to(message, "📊 В этом чате еще никто не установил пол")
        logger.info(f'Команда: genderstats | Пользователь: {message.from_user.username} | Ошибка: Данные отсутствуют')
        return

    males = 0
    females = 0

    for user_gender in gender_data[str(chat_id)].values():
        if user_gender == "male":
            males += 1
        else:
            females += 1

    total = males + females
    bot.reply_to(message, 
                f"📊 Статистика полов в этом чате:\n"
                f"♂️ Мужчины: {males} ({males/total*100:.1f}%)\n"
                f"♀️ Женщины: {females} ({females/total*100:.1f}%)\n"
                f"👥 Всего: {total}")

# Функция для получения пола пользователя
def get_user_gender(chat_id, user_id):
    chat_id_str = str(chat_id)
    user_id_str = str(user_id)

    if (chat_id_str in gender_data and 
        user_id_str in gender_data[chat_id_str]):
        return gender_data[chat_id_str][user_id_str]
    return None

# Функция для получения правильного окончания в зависимости от пола
def get_gender_suffix(chat_id, user_id, male_suffix, female_suffix):
    gender = get_user_gender(chat_id, user_id)
    if gender == "male":
        return male_suffix
    elif gender == "female":
        return female_suffix
    else:
        return male_suffix + "/" + female_suffix  # если пол не установлен

# глав. команды

@bot.message_handler(content_types=['text'])
def text(message):
    if message.text in ['a:help', 'akame:help']:
        bot.send_message(message.chat.id, '''
╔══════════════════╗
║              🎀 ПОМОЩЬ            ║
╚══════════════════╝

🎯 𝐎𝐂𝐍𝐎𝐁𝐍𝐎𝐄
├─ ❓ a:help » помощь
├─ ⚙️ a:cmd » команды
├─ ℹ️ a:info » информация
├─ ⚧ /setgender » пол
├─ 👤 /mygender » мой пол
└─ 📊 /genderstats » статистика

💞 𝐑𝐏-𝐊𝐎𝐌𝐀𝐍𝐃𝐘
├─ 💋 a:kiss [user] » поцеловать
├─ 🤗 a:hug [user] » обнять
├─ 🐱 a:myr [user] » мурчать
├─ 😴 a:sleep [user] » спать
└─ 🥰 a:pat [user] » погладить

💡 𝐏𝐑𝐈𝐌𝐄𝐂𝐇𝐀𝐍𝐈𝐄
✨ Префикс *akame:* = *a:*
  Пример: *akame:hug [user]*

✦ ─── ⋆⋅☆⋅⋆ ─── ✦

        ''')
        logger.info(f'Команда: help | Пользователь: {message.from_user.username}')
    elif message.text in ['a:cmd', 'akame:cmd']:
        bot.send_message(message.chat.id, '''
╔══════════════════╗
║           🎯 КОМАНДЫ             ║
╚══════════════════╝

🎮 𝐎𝐒𝐍𝐎𝐕𝐍𝐘𝐄
├─ ❓ a:help » помощь
├─ ⚙️ a:cmd » команды
└─ ℹ️ a:info » информация

💞 𝐑𝐏-𝐊𝐎𝐌𝐀𝐍𝐃𝐘  
├─ 💋 a:kiss » поцеловать
├─ 🤗 a:hug » обнять
├─ 🐱 a:myr » помурчать
├─ 😴 a:sleep » спать
└─ 🥰 a:pat » погладить

👤 𝐆𝐄𝐍𝐃𝐄𝐑
├─ ⚧ /setgender » установить пол
├─ 👤 /mygender » мой пол
└─ 📊 /genderstats » статистика

✦ ─── ⋆⋅☆⋅⋆ ─── ✦''')
        logger.info(f'Команда: cmd | Пользователь: {message.from_user.username}')
    elif message.text in ['a:info', 'akame:info']:
        bot.send_message(message.chat.id, '''
╔═══════════════════════╗
║                  👨‍💻 Персонал                       ║
╚═══════════════════════╝
🔥Создатель: @treplebeska
💄Бета-тестер:
🤚Хелпер: @Broiashka9
''')
        logger.info(f'Команда: info | Пользователь: {message.from_user.username}')
    # рп команды

    # поцеловать

    elif message.text.startswith(('a:kiss', 'akame:kiss')):
        target = message.text.replace('a:kiss', '').replace('akame:kiss', '').strip()
        chat_id = message.chat.id
        user_id = message.from_user.id
        if not target:
            bot.reply_to(message, '❌ Укажи кого поцеловать: `a:kiss [имя]`')
            logger.info(f'Команда: kiss | Пользователь: {message.from_user.username} | Ошибка: Цель не указана')
            return

        kisser = f"@{message.from_user.username}" if message.from_user.username else message.from_user.first_name

        user_gender = get_user_gender(message.chat.id, message.from_user.id)
        if user_gender == "male":
            action = random.choice(['поцеловал', 'поцеловал в щеку'])
        elif user_gender == "female":
            action = random.choice(['поцеловала', 'поцеловала в щеку'])
        else:
            action = random.choice(['поцеловал(а)', 'поцеловал(а) в щеку'])

        bot.reply_to(message, f'🤗 {kisser} {action} {target}!')
        logger.info(f'Команда: kiss | Пользователь: {message.from_user.username} | Цель: {target}')

    # обнять

    elif message.text.startswith(('a:hug', 'akame:hug')):
        target = message.text.replace('a:hug', '').replace('akame:hug', '').strip()
        chat_id = message.chat.id
        user_id = message.from_user.id
        if not target:
            bot.reply_to(message, '❌ Укажи кого обнять: `a:hug [имя]`')
            logger.info(f'Команда: hug | Пользователь: {message.from_user.username} | Ошибка: Цель не указана')
            return

        hugger = f"@{message.from_user.username}" if message.from_user.username else message.from_user.first_name

        user_gender = get_user_gender(message.chat.id, message.from_user.id)
        if user_gender == "male":
            action = "обнял"
        elif user_gender == "female":
            action = "обняла"
        else:
            action = "обнял(а)"  # если пол не установлен

        bot.reply_to(message, f'🤗 {hugger} {action} {target}!')
        logger.info(f'Команда: hug | Пользователь: {message.from_user.username} | Цель: {target}')

    # помурчать 

    elif message.text.startswith(('a:myr', 'akame:myr')):
        target = message.text.replace('a:myr', '').replace('akame:myr', '').strip()
        chat_id = message.chat.id
        user_id = message.from_user.id
        if not target:
            bot.reply_to(message, '❌ Укажи кому помурчать: `a:myr [имя]`')
            logger.info(f'Команда: myr | Пользователь: {message.from_user.username} | Ошибка: Цель не указана')
            return

        myrer = f"@{message.from_user.username}" if message.from_user.username else message.from_user.first_name

        user_gender = get_user_gender(message.chat.id, message.from_user.id)
        if user_gender == "male":
            action = random.choice(['помурчал'])
        elif user_gender == "female":
            action = random.choice(['помурчала'])
        else:
            action = random.choice(['помурчал(а)'])

        bot.reply_to(message, f'😻 {myrer} {action} {target}!')
        logger.info(f'Команда: myr | Пользователь: {message.from_user.username} | Цель: {target}')

    # спать

    elif message.text.startswith(('a:sleep', 'akame:sleep')):
        target = message.text.replace('a:sleep', '').replace('akame:sleep', '').strip()
        if not target:
            bot.reply_to(message, '❌ Укажи с кем спать: `a:sleep [имя]`')
            logger.info(f'Команда: sleep | Пользователь: {message.from_user.username} | Ошибка: Цель не указана')
            return

        sleeper= f"@{message.from_user.username}" if message.from_user.username else message.from_user.first_name

        bot.reply_to(message, f'🛌 {sleeper} спит с {target}!')
        logger.info(f'Команда: sleep | Пользователь: {message.from_user.username} | Цель: {target}')

    # погладить

    elif message.text.startswith(('a:pat', 'akame:pat')):
        target = message.text.replace('a:pat', '').replace('akame:pat', '').strip()
        chat_id = message.chat.id
        user_id = message.from_user.id
        if not target:
            bot.reply_to(message, '❌ Укажи кого погладить: `a:pat [имя]`')
            logger.info(f'Команда: pat | Пользователь: {message.from_user.username} | Ошибка: Цель не указана')
            return

        pater = f"@{message.from_user.username}" if message.from_user.username else message.from_user.first_name

        user_gender = get_user_gender(message.chat.id, message.from_user.id)
        if user_gender == "male":
            action = random.choice(['погладил', 'погладил по голове'])
        elif user_gender == "female":
            action = random.choice(['погладила', 'погладила по голове'])
        else:
            action = random.choice(['погладил(а)', 'погладил(а) по голове'])

        bot.reply_to(message, f'👋 {pater} {action} {target}!')
        logger.info(f'Команда: pat | Пользователь: {message.from_user.username} | Цель: {target}')


print('INFO: Бот запущен!')
print('INFO: Создатель: @treplebeska')
print('INFO: Версия: 2.0')
print('INFO: Язык  Python')
print('INFO: Старый бот - @akkamee_bot')

bot.infinity_polling()
