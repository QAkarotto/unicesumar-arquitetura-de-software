package br.edu.foodnow.model;

public class Notificacao {
    private final String destinatario;
    private final String assunto;
    private final String mensagem;

    public Notificacao(String destinatario, String assunto, String mensagem) {
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.mensagem = mensagem;
    }

    public String getDestinatario() { return destinatario; }
    public String getAssunto() { return assunto; }
    public String getMensagem() { return mensagem; }

    public Notificacao adicionarReferenciaGeografica(Endereco endereco) {
        String referencia = endereco.classificarZonaDeEntrega() + " ["
                + endereco.getLocalizacao().formatarParaProvedor() + "]";
        return new Notificacao(destinatario, assunto, mensagem + " Região de entrega: " + referencia + ".");
    }
}
