const { Telegraf, Markup } = require('telegraf');
const fs = require('fs').promises;

const bot = new Telegraf('8351445452:AAEUwAJMPYaam8VTUVgyZVGCplnj6Sey7Ok');

// Списки действий
const kissActions = ["поцеловал(а)", "чмокнул(а)", "нежно поцеловал(а)"];
const actions = [
    "убил(а)", "ликвидировал(а)", "уничтожил(а)",
    "прикончил(а)", "отправил(а) на тот свет", "лишил(а) жизни"
];
const hugs = ["обнял(а)", "прижался(ась) к", "обнял(а) крепко", "обнял(а) нежно"];

// Гифки
const kissGifs = [
    "https://media.giphy.com/media/v1.Y2lkPWVjZjA1ZTQ3NXFvODJzOG85aXh0eXl0aG9ia3Q4NWh1bTdsbzY4ejMwNWE4cmc1cCZlcD12MV9naWZzX3JlbGF0ZWQmY3Q9Zw/jR22gdcPiOLaE/giphy.gif",
    "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExM2R0cTlsaTl2bWxzNHc0OXhxY3JoZm51enlhZ3Vmempkd2tlMmVnbSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/zkppEMFvRX5FC/giphy.gif",
];

const hugGifs = [
    "https://media.giphy.com/media/od5H3PmEG5EVq/giphy.gif",
    "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExdGZ5Z3locDlrOTR4ZjJmc2dxdGIxMHc3dGx4ZnViY29xdHRjeDNrMCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/u9BxQbM5bxvwY/giphy.gif",
];

// Функция для логирования
async function logCommand(userId, username, command, target = null, chatType = "private") {
    const timestamp = new Date().toISOString().replace('T', ' ').substring(0, 19);
    let logMessage = `[${timestamp}] Юзер: @${username}, Команда: ${command}, Чат: ${chatType}`;
    if (target) {
        logMessage += `, Цель: ${target}`;
    }

    console.log(logMessage);

    try {
        await fs.appendFile('bot_logs.txt', logMessage + '\n');
    } catch (error) {
        console.error('Ошибка записи в лог:', error);
    }
}

// Создание клавиатуры
function createMainKeyboard() {
    return Markup.inlineKeyboard([
        [Markup.button.callback('Помощь', 'help3')],
        [Markup.button.callback('Новинка', 'new3')]
    ]);
}

// Обработчик команды /start
bot.start(async (ctx) => {
    const user = ctx.from;
    await logCommand(user.id, user.username || user.first_name, "/start");
    
    await ctx.reply(
        'Привет я Акаме.\nЯ создана чтобы добавить развлечения в телеграмм группу или сервер развлечения!',
        createMainKeyboard()
    );
});

// Обработчик команды help
bot.hears(/^akame:help/i, async (ctx) => {
    const user = ctx.from;
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    await logCommand(user.id, user.username || user.first_name, "akame:help", null, chatType);
    
    const helpText = `Тут можно узнать команды и их использование

akame:kill [юзернейм] - убить человека
akame:new - новые команды
akame:hug [юзернейм] - обнять человека
akame:kiss [юзернейм] - поцеловать человека

🔥Создатель @treplebeska
Данный бот приватный придобавлении на другой севрер или группу будет выключен!`;

    await ctx.reply(helpText);
});

// Обработчик команды kiss
bot.hears(/^akame:kiss/i, async (ctx) => {
    const user = ctx.from;
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    
    try {
        const text = ctx.message.text;
        const target = text.length > 11 ? text.substring(11).trim() : '';

        if (!target) {
            await ctx.reply("❌ Укажи цель: `akame:kiss [имя]`");
            return;
        }

        const kisser = user.username ? `@${user.username}` : user.first_name;
        const kissAction = kissActions[Math.floor(Math.random() * kissActions.length)];
        const gifUrl = kissGifs[Math.floor(Math.random() * kissGifs.length)];

        const kissMessage = `💋 ${kisser} ${kissAction} ${target}!`;
        await logCommand(user.id, user.username || user.first_name, "akame:kiss", target, chatType);

        try {
            await ctx.replyWithAnimation(gifUrl, { caption: kissMessage });
        } catch (gifError) {
            console.error('Ошибка отправки гифки:', gifError);
            await ctx.reply(kissMessage);
        }
    } catch (error) {
        await logCommand(user.id, user.username || user.first_name, "akame:kiss_error", error.message, chatType);
        await ctx.reply(`❌ Ошибка: ${error.message}`);
    }
});

// Обработчик команды kill
bot.hears(/^akame:kill/i, async (ctx) => {
    const user = ctx.from;
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    
    try {
        const text = ctx.message.text;
        const target = text.length > 11 ? text.substring(11).trim() : '';

        if (!target) {
            await ctx.reply("❌ Укажи цель: `akame:kill [имя]`");
            return;
        }

        const killer = user.username ? `@${user.username}` : user.first_name;
        const action = actions[Math.floor(Math.random() * actions.length)];

        // Экранирование Markdown
        const escapeMarkdown = (text) => text.replace(/([_*[\]()~`>#+-=|{}.!])/g, '\\$1');
        
        const killMessage = `⚔️ **${escapeMarkdown(killer)}** ${action} **${escapeMarkdown(target)}**!`;
        await logCommand(user.id, user.username || user.first_name, "akame:kill", target, chatType);

        await ctx.replyWithMarkdownV2(killMessage);
    } catch (error) {
        await logCommand(user.id, user.username || user.first_name, "akame:kill_error", error.message, chatType);
        await ctx.reply(`❌ Ошибка: ${error.message}`);
    }
});

// Обработчик команды hug
bot.hears(/^akame:hug/i, async (ctx) => {
    const user = ctx.from;
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    
    try {
        const text = ctx.message.text;
        const target = text.length > 10 ? text.substring(10).trim() : '';

        if (!target) {
            await ctx.reply("❌ Укажи цель: `akame:hug [имя]`");
            return;
        }

        const huger = user.username ? `@${user.username}` : user.first_name;
        const hug = hugs[Math.floor(Math.random() * hugs.length)];
        const gifUrl = hugGifs[Math.floor(Math.random() * hugGifs.length)];

        const hugMessage = `🤗 ${huger} ${hug} ${target}!`;
        await logCommand(user.id, user.username || user.first_name, "akame:hug", target, chatType);

        try {
            await ctx.replyWithAnimation(gifUrl, { caption: hugMessage });
        } catch (gifError) {
            console.error('Ошибка отправки гифки:', gifError);
            await ctx.reply(hugMessage);
        }
    } catch (error) {
        await logCommand(user.id, user.username || user.first_name, "akame:hug_error", error.message, chatType);
        await ctx.reply(`❌ Ошибка: ${error.message}`);
    }
});

// Обработчик команды new
bot.hears(/^akame:new/i, async (ctx) => {
    const user = ctx.from;
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    await logCommand(user.id, user.username || user.first_name, "akame:new", null, chatType);
    
    const newText = `Новинка!!!

Последнее обновление 02.10.2025

Добавлены команды:
akame:kill
akame:hug
akame:kiss

Глобальные обновления:
Добавлены гифки к командам akame:hug и akame:kiss`;

    await ctx.reply(newText);
});

// Обработчик кнопок
bot.action('help3', async (ctx) => {
    const user = ctx.from;
    await logCommand(user.id, user.username || user.first_name, "help_button");
    
    const helpText = `Тут можно узнать команды и их использование

akame:kill [юзернейм] - убить человека
akame:new - новые команды
akame:hug [юзернейм] - обнять человека
akame:kiss [юзернейм] - поцеловать человека

🔥Создатель @treplebeska
Данный бот приватный придобавлении на другой севрер или группу будет выключен!`;

    await ctx.editMessageText(helpText, createMainKeyboard());
    await ctx.answerCbQuery();
});

bot.action('new3', async (ctx) => {
    const user = ctx.from;
    await logCommand(user.id, user.username || user.first_name, "new_commands_button");
    
    const newText = `Новинка!!!

Последнее обновление 02.10.2025

Добавлены команды:
akame:kill
akame:hug
akame:kiss

Глобальные обновления:
Добавлены гифки к командам akame:hug и akame:kiss`;

    await ctx.editMessageText(newText, createMainKeyboard());
    await ctx.answerCbQuery();
});

// Обработчик групповых команд с упоминанием бота
bot.on('text', async (ctx) => {
    const chatType = ctx.chat.type === 'group' || ctx.chat.type === 'supergroup' ? 'group' : 'private';
    
    if (chatType === 'group') {
        const text = ctx.message.text.toLowerCase();
        const botUsername = ctx.botInfo.username.toLowerCase();
        
        if (text.includes(`@${botUsername}`)) {
            const botText = text.replace(`@${botUsername}`, '').trim();
            const user = ctx.from;

            if (botText.startsWith('kill ')) {
                await handleGroupKill(ctx, botText.substring(5).trim(), user);
            } else if (botText.startsWith('hug ')) {
                await handleGroupHug(ctx, botText.substring(4).trim(), user);
            } else if (botText.startsWith('help')) {
                await handleGroupHelp(ctx, user);
            }
        }
    }
});

async function handleGroupKill(ctx, target, user) {
    const chatType = "group";

    if (!target) {
        await ctx.reply("❌ Укажи цель: `kill [имя]`");
        return;
    }

    const killer = user.username ? `@${user.username}` : user.first_name;
    const action = actions[Math.floor(Math.random() * actions.length)];

    const escapeMarkdown = (text) => text.replace(/([_*[\]()~`>#+-=|{}.!])/g, '\\$1');
    const killMessage = `⚔️ **${escapeMarkdown(killer)}** ${action} **${escapeMarkdown(target)}**!`;
    
    await logCommand(user.id, user.username || user.first_name, "akame:kill", target, chatType);
    await ctx.replyWithMarkdownV2(killMessage);
}

async function handleGroupHug(ctx, target, user) {
    const chatType = "group";

    if (!target) {
        await ctx.reply("❌ Укажи цель: `hug [имя]`");
        return;
    }

    const huger = user.username ? `@${user.username}` : user.first_name;
    const hug = hugs[Math.floor(Math.random() * hugs.length)];
    const gifUrl = hugGifs[Math.floor(Math.random() * hugGifs.length)];

    const hugMessage = `🤗 ${huger} ${hug} ${target}!`;
    await logCommand(user.id, user.username || user.first_name, "akame:hug", target, chatType);

    try {
        await ctx.replyWithAnimation(gifUrl, { caption: hugMessage });
    } catch (gifError) {
        console.error('Ошибка отправки гифки:', gifError);
        await ctx.reply(hugMessage);
    }
}

async function handleGroupHelp(ctx, user) {
    const chatType = "group";
    await logCommand(user.id, user.username || user.first_name, "akame:help", null, chatType);
    
    const helpText = `Тут можно узнать команды и их использование

akame:kill [юзернейм] - убить человека
akame:new - новые команды
akame:hug [юзернейм] - обнять человека
akame:kiss [юзернейм] - поцеловать человека

🔥Создатель @treplebeska`;

    await ctx.reply(helpText);
}

// Обработка ошибок
bot.catch((err, ctx) => {
    console.error(`Ошибка для ${ctx.updateType}:`, err);
});

// Запуск бота
console.log('Бот запускается...');
bot.launch().then(() => {
    console.log('Бот успешно запущен!');
});

// Graceful shutdown
process.once('SIGINT', () => bot.stop('SIGINT'));
process.once('SIGTERM', () => bot.stop('SIGTERM'));