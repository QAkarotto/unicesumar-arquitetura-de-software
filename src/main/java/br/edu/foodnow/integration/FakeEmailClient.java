package br.edu.foodnow.integration;

import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.notificacao.EnvioNotificacao;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FakeEmailClient implements EnvioNotificacao {
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
