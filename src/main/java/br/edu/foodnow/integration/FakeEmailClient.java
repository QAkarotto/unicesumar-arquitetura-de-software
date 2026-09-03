package br.edu.foodnow.integration;

import br.edu.foodnow.model.Notificacao;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class FakeEmailClient {
    private final List<Notificacao> enviadas = new CopyOnWriteArrayList<>();

    public void enviar(String destinatario, String assunto, String mensagem) {
        enviadas.add(new Notificacao(destinatario, assunto, mensagem));
    }

    public List<Notificacao> getEnviadas() {
        return List.copyOf(enviadas);
    }

    public void limpar() {
        enviadas.clear();
    }
}
