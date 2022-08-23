package iafenvoy.accountswitcher.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import iafenvoy.accountswitcher.login.OfflineLogin;
import iafenvoy.accountswitcher.utils.FileUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AccountManager {
    public static final AccountManager INSTANCE = new AccountManager();
    public static final MinecraftClient client = MinecraftClient.getInstance();
    private static final String FILE_PATH = "./config/accounts.json";
    public static Account CURRENT = null;
    private final List<Account> accounts = new ArrayList<>();

    public AccountManager() {

    }

    public static void setAccountFromClient() {
        CURRENT = AccountManager.INSTANCE.getAccountByUuid(client.getSession().getUuid());
    }

    public static Text getAccountInfoText() {
        String type;
        if (AccountManager.CURRENT == null)
            type = "Error";
        else
            type = AccountManager.CURRENT.getType().getName();
        return new TranslatableText("as.titleScreen.nowUse", client.getSession().getUsername(), type);
    }

    public void load() {
        try {
            String data = FileUtil.readFile(FILE_PATH);
            JsonArray json = new JsonParser().parse(data).getAsJsonArray();
            for (JsonElement ele : json) {
                JsonObject obj = ele.getAsJsonObject();
                Account.AccountType type = Account.AccountType.getByName(obj.get("type").getAsString());
                if (type == Account.AccountType.Offline)
                    this.accounts.add(OfflineLogin.generateAccount(obj.get("username").getAsString()));
                else if (type == Account.AccountType.Microsoft) {
                    String accessToken = obj.get("accessToken").getAsString();
                    String refreshToken = obj.get("refreshToken").getAsString();
                    String username = obj.get("username").getAsString();
                    String uuid = obj.get("uuid").getAsString();
                    this.accounts.add(new Account(type, accessToken, refreshToken, username, uuid));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        try {
            JsonArray array = new JsonArray();
            for (Account account : this.accounts) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", account.getType().getKey());
                if (account.getType() == Account.AccountType.Offline)
                    obj.addProperty("username", account.getUsername());
                else if (account.getType() == Account.AccountType.Microsoft) {
                    obj.addProperty("accessToken", account.getAccessToken());
                    obj.addProperty("refreshToken", account.getRefreshToken());
                    obj.addProperty("username", account.getUsername());
                    obj.addProperty("uuid", account.getUuid());
                }
                array.add(obj);
            }
            FileUtil.saveFile(FILE_PATH, array.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAccount(@NotNull Account account) {
        for (int i = 0; i < this.accounts.size(); i++)
            if (this.accounts.get(i).getUuid().equals(account.getUuid())) {
                this.accounts.set(i, account);
                return;
            }
        this.accounts.add(account);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void deleteAccountByUuid(String uuid) {
        int index = -1;
        for (int i = 0; i < this.accounts.size(); i++)
            if (this.accounts.get(i).getUuid().equals(uuid)) {
                index = i;
                break;
            }
        if (index >= 0)
            this.accounts.remove(index);
    }

    public Account getAccountByUuid(String uuid) {
        for (Account account : this.accounts)
            if (account.getUuid().equals(uuid))
                return account;
        return null;
    }
}