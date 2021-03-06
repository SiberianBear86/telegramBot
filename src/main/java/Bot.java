import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {
    private Map <Long, GameQuiz> chatId = new HashMap <>();

    public static void main(String[] args) {
        Config.load();
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e1) {
            e1.printStackTrace();
        }
    }

    private void sendMsg(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        try {
            CreateButtons.setButtons(sendMessage);
            sendMessage(sendMessage);
        } catch (TelegramApiException e1) {
            e1.printStackTrace();
        }
    }

    private void sendPht(Long chatId, File picture){
        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(chatId);
        sendPhotoRequest.setNewPhoto(picture);
        try {
            sendPhoto(sendPhotoRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (update.hasCallbackQuery()) {
            CallbackQuery callback = update.getCallbackQuery();
            Long id = callback.getMessage().getChatId();
            chatId.computeIfAbsent(id, k -> {
                try {
                    return new GameQuiz(callback.getData());
                } catch (IOException e) {
                    sendMsg(id, e.getMessage());
                }
                return null;
            });
            GameQuiz game = chatId.get(id);
            WorkWithQuery.workWithQuest(callback.getData(), id, game);
            sendMsg(id, WorkWithQuery.botAnswer);
            if (game.getAmountQuest() != 0 && game.getPoint() >= 0) {
                if (game.theme.equals("Кино"))
                    sendPht(id, game.question.picture);
                try {
                    execute(WorkWithQuery.newQuest);
                } catch (TelegramApiException e) {
                    sendMsg(id, e.getMessage());
                }
            } else {
                sendMsg(id, WorkWithQuery.gameOver);
                chatId.remove(id);
            }
        } else if (message != null && message.hasText()) {
            Long id = message.getChatId();
            Commands com = Commands.parse(message.getText().toUpperCase());
            if (com != null) {
                sendMsg(id, com.command.getBotAnswer(id, chatId.get(id), chatId));
                try {
                    execute(com.command.trySendMsg(id, chatId.get(id)));
                } catch (TelegramApiException e) {
                    sendMsg(id, e.getMessage());
                }
            } else
                sendMsg(id, "Не знаю такую команду");
        }
    }

    public String getBotUsername() {
        return Config.botName;
    }

    public String getBotToken() {
        return Config.botToken;
    }
}