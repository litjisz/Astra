package lol.jisz.astra.test;

import lol.jisz.astra.Astra;
import lol.jisz.astra.command.CommandBase;
import lol.jisz.astra.command.CommandManager;
import lol.jisz.astra.utils.Logger;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandTests {
    
    private final Astra plugin;
    private final Logger logger;
    private final CommandManager commandManager;
    
    public CommandTests(Astra plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger();
        this.commandManager = plugin.getCommandManager();
    }

    public void runTests() {
        testManualRegistration();
        testAutoRegistration();
        testCommandUnregistration();
    }

    private void testManualRegistration() {
        try {
            TestCommand command = new TestCommand();
            plugin.registerCommand(command);
            
            if (commandManager.isRegistered("testcommand")) {
                logger.info("✓ Prueba de registro manual de comando exitosa");
            } else {
                logger.error("✗ Prueba de registro manual de comando fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de registro manual de comando", e);
        }
    }

    private void testAutoRegistration() {
        try {
            if (commandManager.isRegistered("autocommand")) {
                logger.info("✓ Prueba de registro automático de comando exitosa");
            } else {
                logger.error("✗ Prueba de registro automático de comando fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de registro automático de comando", e);
        }
    }

    private void testCommandUnregistration() {
        try {
            TestCommand command = new TestCommand();
            plugin.registerCommand(command);
            
            plugin.unregisterCommand(command);
            
            if (!commandManager.isRegistered("testcommand")) {
                logger.info("✓ Prueba de desregistro de comando exitosa");
            } else {
                logger.error("✗ Prueba de desregistro de comando fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de desregistro de comando", e);
        }
    }

    public static class TestCommand extends CommandBase {
        
        public TestCommand() {
            super("testcommand", "astra.test.command", false, Arrays.asList("testcmd"));
        }
        
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            sender.sendMessage("Test command executed!");
            return true;
        }
    }
}
