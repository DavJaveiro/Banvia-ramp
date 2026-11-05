package com.banvia;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verificaFronteirasDosModulos() {
        ApplicationModules.of(BanviaApplication.class).verify();
    }
}
