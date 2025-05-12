package lol.jisz.astra.test;

import lol.jisz.astra.Astra;
import lol.jisz.astra.utils.Logger;

public class AstraTestRunner extends Astra {
    
    private static final String TEST_PREFIX = "[AstraTest] ";
    
    @Override
    protected void onInitialize() {
        Logger logger = logger();
        
        logger.info(TEST_PREFIX + "Iniciando pruebas de Astra...");
        
        runModuleTests();
        runCommandTests();
        
        logger.info(TEST_PREFIX + "Pruebas completadas.");
    }
    
    @Override
    protected void onShutdown() {
        logger().info(TEST_PREFIX + "Finalizando pruebas de Astra.");
    }
    
    @Override
    protected void onReload() {
        logger().info(TEST_PREFIX + "Recargando pruebas de Astra.");
    }

    private void runModuleTests() {
        logger().info(TEST_PREFIX + "Ejecutando pruebas de módulos...");
        new ModuleTests(this).runTests();
    }

    private void runCommandTests() {
        logger().info(TEST_PREFIX + "Ejecutando pruebas de comandos...");
        new CommandTests(this).runTests();
    }
}