package lol.jisz.astra.test;

import lol.jisz.astra.Astra;
import lol.jisz.astra.api.AutoRegisterModule;
import lol.jisz.astra.api.Implements;
import lol.jisz.astra.api.Module;
import lol.jisz.astra.utils.Logger;

/**
 * Pruebas para el sistema de m&oacute;dulos de Astra.
 * Verifica el registro, habilitaci&oacute;n, deshabilitaci&oacute;n y recarga de m&oacute;dulos.
 */
public class ModuleTests {
    
    private final Astra plugin;
    private final Logger logger;
    
    public ModuleTests(Astra plugin) {
        this.plugin = plugin;
        this.logger = plugin.logger();
    }
    
    /**
     * Ejecuta todas las pruebas relacionadas con m&oacute;dulos
     */
    public void runTests() {
        testManualRegistration();
        testAutoRegistration();
        testModuleLifecycle();
        testModuleFetch();
    }
    
    /**
     * Prueba el registro manual de m&oacute;dulos
     */
    private void testManualRegistration() {
        try {
            TestModule module = new TestModule(plugin);
            plugin.registerModule(module);
            
            if (Implements.isRegistered(TestModule.class)) {
                logger.info("✓ Prueba de registro manual de m&oacute;dulo exitosa");
            } else {
                logger.error("✗ Prueba de registro manual de m&oacute;dulo fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de registro manual de m&oacute;dulo", e);
        }
    }
    
    /**
     * Prueba el registro autom&aacute;tico de m&oacute;dulos
     */
    private void testAutoRegistration() {
        try {
            // El m&oacute;dulo AutoRegisteredModule deber&iacute;a registrarse autom&aacute;ticamente
            // durante el escaneo de paquetes
            if (Implements.isRegistered(AutoRegisteredModule.class)) {
                logger.info("✓ Prueba de registro autom&aacute;tico de m&oacute;dulo exitosa");
            } else {
                logger.error("✗ Prueba de registro autom&aacute;tico de m&oacute;dulo fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de registro autom&aacute;tico de m&oacute;dulo", e);
        }
    }
    
    /**
     * Prueba el ciclo de vida de los m&oacute;dulos (enable, disable, reload)
     */
    private void testModuleLifecycle() {
        try {
            TestModule module = new TestModule(plugin);
            plugin.registerModule(module);
            
            module.enable();
            if (module.isEnabled()) {
                logger.info("✓ Prueba de habilitaci&oacute;n de m&oacute;dulo exitosa");
            } else {
                logger.error("✗ Prueba de habilitaci&oacute;n de m&oacute;dulo fallida");
            }
            
            module.disable();
            if (!module.isEnabled()) {
                logger.info("✓ Prueba de deshabilitaci&oacute;n de m&oacute;dulo exitosa");
            } else {
                logger.error("✗ Prueba de deshabilitaci&oacute;n de m&oacute;dulo fallida");
            }
            
            module.reload();
            logger.info("✓ Prueba de recarga de m&oacute;dulo exitosa");
            
        } catch (Exception e) {
            logger.error("✗ Error en prueba de ciclo de vida de m&oacute;dulo", e);
        }
    }
    
    /**
     * Prueba la obtenci&oacute;n de m&oacute;dulos registrados
     */
    private void testModuleFetch() {
        try {
            TestModule module = new TestModule(plugin);
            plugin.registerModule(module);
            
            TestModule fetchedModule = Implements.fetch(TestModule.class);
            if (fetchedModule == module) {
                logger.info("✓ Prueba de obtenci&oacute;n de m&oacute;dulo exitosa");
            } else {
                logger.error("✗ Prueba de obtenci&oacute;n de m&oacute;dulo fallida");
            }
        } catch (Exception e) {
            logger.error("✗ Error en prueba de obtenci&oacute;n de m&oacute;dulo", e);
        }
    }
    
    /**
     * M&oacute;dulo de prueba para verificar el sistema de m&oacute;dulos
     */
    public static class TestModule implements Module {
        
        private final Astra plugin;
        private boolean enabled = false;
        
        public TestModule(Astra plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public void enable() {
            enabled = true;
        }
        
        @Override
        public void disable() {
            enabled = false;
        }
        
        @Override
        public void reload() {
            // Simular recarga
        }
        
        public boolean isEnabled() {
            return enabled;
        }
    }
    
    /**
     * M&oacute;dulo de prueba que se registra autom&aacute;ticamente
     */
    @AutoRegisterModule(priority = 1)
    public static class AutoRegisteredModule implements Module {
        
        @Override
        public void enable() {
            // Implementaci&oacute;n de prueba
        }
        
        @Override
        public void disable() {
            // Implementaci&oacute;n de prueba
        }
        
        @Override
        public void reload() {
            // Implementaci&oacute;n de prueba
        }
    }
}