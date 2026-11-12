
package br.edu.foodnow.notificacao;

import br.edu.foodnow.model.Notificacao;

/**
 * Contrato para envio de notificações por e-mail.
 *
 * A camada de serviços depende desta abstração,
 * e não de uma implementação específica de envio.
 */
public interface EmailGateway {

    void enviar(String destinatario, String assunto, String mensagem);

    void enviar(Notificacao notificacao);
}
