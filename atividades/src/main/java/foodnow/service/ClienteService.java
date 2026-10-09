package main.java.foodnow.service;

public class ClienteService {

    public boolean validarCliente(int clienteId) {
        return clienteId > 0;
    }

    public String buscarNome(int clienteId) {
        if (validarCliente(clienteId)) {
            return "Cliente " + clienteId;
        }
        return "Cliente não encontrado";
    }

    public String buscarRegiao(int clienteId) {
        return "Centro";
    }

    public void atualizarEndereco(int clienteId, String endereco) {
        System.out.println("Endereço atualizado para o cliente " + clienteId + ": " + endereco);
    }

    public void notificarCliente(int clienteId, String mensagem) {
        System.out.println("Notificação para o cliente " + clienteId + ": " + mensagem);
    }
}