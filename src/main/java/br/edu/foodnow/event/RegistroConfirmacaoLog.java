
package br.edu.foodnow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegistroConfirmacaoLog {

    private static final Logger log =
            LoggerFactory.getLogger(RegistroConfirmacaoLog.class);

    public void registrar(Long pedidoId) {
        log.info("Pedido confirmado com sucesso. ID: {}", pedidoId);
    }
}
