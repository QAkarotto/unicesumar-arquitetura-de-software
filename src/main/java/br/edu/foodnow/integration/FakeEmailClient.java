package br.edu.foodnow.integration;

import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.notificacao.EmailGateway;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementação simulada do contrato de envio de e-mail.
 *
 * Na arquitetura, funciona como um adaptador de infraestrutura.
 */
@Component
public class FakeEmailClient implements EmailGateway {

    private final List<Notificacao> enviadas = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destinatario, String assunto, String mensagem) {
        enviadas.add(new Notificacao(destinatario, assunto, mensagem));
    }

    @Override
    public void enviar(Notificacao notificacao) {
        enviadas.add(notificacao);
    }

    public List<Notificacao> getEnviadas() {
        return List.copyOf(enviadas);
    }

    public void limpar() {
        enviadas.clear();
    }
}
