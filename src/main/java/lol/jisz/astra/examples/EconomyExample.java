package lol.jisz.astra.examples;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AbstractModule;
import lol.jisz.astra.utils.ConfigManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Example implementation of an economy system as an Astra module.
 * Provides functionality for managing player accounts, balances, and currency operations.
 */
public class EconomyExample extends AbstractModule {

    private ConfigManager configManager;
    private final String ECONOMY_CONFIG = "economy";
    private final String ACCOUNTS_PATH = "accounts";

    /**
     * Constructs a new EconomyExample module.
     *
     * @param plugin The Astra plugin instance this module belongs to
     */
    public EconomyExample(Astra plugin) {
        super(plugin);
        this.configManager = new ConfigManager(plugin);
    }

    /**
     * Initializes the economy module by loading configuration and setting default values.
     * This method is called when the module is enabled.
     */
    @Override
    public void enable() {
        // Load economy configuration
        configManager.loadConfig(ECONOMY_CONFIG);

        // Set default values if necessary
        if (!configManager.getConfig(ECONOMY_CONFIG).contains("settings")) {
            configManager.set(ECONOMY_CONFIG, "settings.starting-balance", 100.0);
            configManager.set(ECONOMY_CONFIG, "settings.currency-symbol", "$");
            configManager.set(ECONOMY_CONFIG, "settings.currency-name", "Coins");
            configManager.set(ECONOMY_CONFIG, "settings.currency-name-plural", "Coins");
            configManager.saveConfig(ECONOMY_CONFIG);
        }
    }

    /**
     * Saves economy data when the module is being disabled.
     * This method is called when the module is disabled.
     */
    @Override
    public void disable() {
        // Save economy data when disabling
        configManager.saveConfig(ECONOMY_CONFIG);
    }

    /**
     * Creates a new economy account for a player if one does not already exist.
     * The account is initialized with the default starting balance from configuration.
     *
     * @param player The player to create an account for
     */
    public void createAccount(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        String accountPath = ACCOUNTS_PATH + "." + uuid.toString();

        if (!configManager.getConfig(ECONOMY_CONFIG).contains(accountPath)) {
            double startingBalance = configManager.getConfig(ECONOMY_CONFIG)
                    .getDouble("settings.starting-balance", 100.0);

            configManager.set(ECONOMY_CONFIG, accountPath + ".balance", startingBalance);
            configManager.set(ECONOMY_CONFIG, accountPath + ".name", player.getName());
            configManager.set(ECONOMY_CONFIG, accountPath + ".created", System.currentTimeMillis());
            configManager.saveConfig(ECONOMY_CONFIG);

            logger().info("Account created for " + player.getName() + " with an initial balance of " + startingBalance);
        }
    }

    /**
     * Retrieves the current balance of a player's account.
     * If the player doesn't have an account, one will be created automatically.
     *
     * @param player The player whose balance to check
     * @return The current balance of the player's account
     */
    public double getBalance(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        String balancePath = ACCOUNTS_PATH + "." + uuid.toString() + ".balance";

        // Create account if it does not exist
        if (!hasAccount(player)) {
            createAccount(player);
        }

        return configManager.getDouble(ECONOMY_CONFIG, balancePath, 0.0);
    }

    /**
     * Sets the balance of a player's account to a specific amount.
     * If the player doesn't have an account, one will be created automatically.
     *
     * @param player The player whose balance to set
     * @param amount The new balance amount to set
     */
    public void setBalance(OfflinePlayer player, double amount) {
        UUID uuid = player.getUniqueId();
        String balancePath = ACCOUNTS_PATH + "." + uuid.toString() + ".balance";

        // Create account if it does not exist
        if (!hasAccount(player)) {
            createAccount(player);
        }

        configManager.set(ECONOMY_CONFIG, balancePath, amount);
        configManager.saveConfig(ECONOMY_CONFIG);
    }

    /**
     * Adds a specified amount to a player's account balance.
     * Notifies the player if they are online.
     *
     * @param player The player to deposit money to
     * @param amount The amount to deposit (must be positive)
     * @return true if the deposit was successful, false if the amount was negative
     */
    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }

        double balance = getBalance(player);
        setBalance(player, balance + amount);

        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            String symbol = configManager.getString(ECONOMY_CONFIG, "settings.currency-symbol", "$");
            onlinePlayer.sendMessage("§a+" + symbol + amount + " added to your account");
        }

        return true;
    }

    /**
     * Withdraws a specified amount from a player's account balance.
     * The withdrawal will fail if the player has insufficient funds.
     * Notifies the player if they are online.
     *
     * @param player The player to withdraw money from
     * @param amount The amount to withdraw (must be positive)
     * @return true if the withdrawal was successful, false if the amount was negative or insufficient funds
     */
    public boolean withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }

        double balance = getBalance(player);

        if (balance < amount) {
            return false;
        }

        setBalance(player, balance - amount);

        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            String symbol = configManager.getString(ECONOMY_CONFIG, "settings.currency-symbol", "$");
            onlinePlayer.sendMessage("§c-" + symbol + amount + " withdrawn from your account");
        }

        return true;
    }

    /**
     * Checks if a player has an economy account.
     *
     * @param player The player to check
     * @return true if the player has an account, false otherwise
     */
    public boolean hasAccount(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        String accountPath = ACCOUNTS_PATH + "." + uuid.toString();
        return configManager.getConfig(ECONOMY_CONFIG).contains(accountPath);
    }

    /**
     * Formats a monetary amount with the configured currency symbol.
     *
     * @param amount The amount to format
     * @return A string representation of the amount with the currency symbol
     */
    public String format(double amount) {
        String symbol = configManager.getString(ECONOMY_CONFIG, "settings.currency-symbol", "$");
        return symbol + amount;
    }
}