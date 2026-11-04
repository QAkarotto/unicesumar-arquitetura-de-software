
package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakePaymentGateway;
import br.edu.foodnow.model.*;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagamentoServiceTest {

    @Test
    void deveTratarFalhaDoProvedorDePagamento() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        PagamentoRepository pagamentos = mock(PagamentoRepository.class);
        FakePaymentGateway gateway = mock(FakePaymentGateway.class);

        Pedido pedido = mock(Pedido.class);
        Restaurante restaurante = mock(Restaurante.class);
        Endereco enderecoRestaurante = mock(Endereco.class);
        Endereco enderecoEntrega = mock(Endereco.class);
        Localizacao localizacaoRestaurante = mock(Localizacao.class);
        Localizacao localizacaoEntrega = mock(Localizacao.class);

        when(pedidos.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedido.getStatus()).thenReturn(StatusPedido.CONFIRMADO);
        when(pedido.getRestaurante()).thenReturn(restaurante);
        when(restaurante.getEndereco()).thenReturn(enderecoRestaurante);
        when(pedido.getEnderecoEntrega()).thenReturn(enderecoEntrega);
        when(enderecoRestaurante.getLocalizacao()).thenReturn(localizacaoRestaurante);
        when(enderecoEntrega.getLocalizacao()).thenReturn(localizacaoEntrega);
        when(localizacaoRestaurante.formatarParaProvedor()).thenReturn("origem");
        when(localizacaoEntrega.formatarParaProvedor()).thenReturn("destino");
        when(pedido.getId()).thenReturn(1L);
        when(pedido.getValorTotal()).thenReturn(BigDecimal.TEN);

        when(gateway.processarPagamento(any(FakePaymentGateway.GatewayRequest.class)))
                .thenThrow(new RuntimeException("Provedor indisponivel"));

        PagamentoService service = new PagamentoService(
                pedidos, pagamentos, gateway,
                mock(NotificacaoService.class),
                mock(EntregaService.class),
                mock(LocalizacaoService.class),
                mock(ValidadorEntrega.class)
        );

        assertThrows(
                FalhaIntegracaoException.class,
                () -> service.processar(1L, FormaPagamento.PIX, "TOKEN")
        );
    }

    @Test
    void deveTratarStatusDesconhecidoDoProvedor() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        PagamentoRepository pagamentos = mock(PagamentoRepository.class);
        FakePaymentGateway gateway = mock(FakePaymentGateway.class);

        NotificacaoService notificacao = mock(NotificacaoService.class);
        EntregaService entrega = mock(EntregaService.class);

        Pedido pedido = mock(Pedido.class);
        Restaurante restaurante = mock(Restaurante.class);
        Endereco enderecoRestaurante = mock(Endereco.class);
        Endereco enderecoEntrega = mock(Endereco.class);
        Localizacao localizacaoRestaurante = mock(Localizacao.class);
        Localizacao localizacaoEntrega = mock(Localizacao.class);

        when(pedidos.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedido.getStatus()).thenReturn(StatusPedido.CONFIRMADO);
        when(pedido.getRestaurante()).thenReturn(restaurante);
        when(restaurante.getEndereco()).thenReturn(enderecoRestaurante);
        when(pedido.getEnderecoEntrega()).thenReturn(enderecoEntrega);
        when(enderecoRestaurante.getLocalizacao()).thenReturn(localizacaoRestaurante);
        when(enderecoEntrega.getLocalizacao()).thenReturn(localizacaoEntrega);
        when(localizacaoRestaurante.formatarParaProvedor()).thenReturn("origem");
        when(localizacaoEntrega.formatarParaProvedor()).thenReturn("destino");
        when(pedido.getId()).thenReturn(1L);
        when(pedido.getValorTotal()).thenReturn(BigDecimal.TEN);

        when(gateway.processarPagamento(any(FakePaymentGateway.GatewayRequest.class)))
                .thenReturn(new FakePaymentGateway.GatewayResult(
                        "FN-1", "UNKNOWN", "Status desconhecido"
                ));

        PagamentoService service = new PagamentoService(
                pedidos, pagamentos, gateway,
                notificacao, entrega,
                mock(LocalizacaoService.class),
                mock(ValidadorEntrega.class)
        );

        assertThrows(
                FalhaIntegracaoException.class,
                () -> service.processar(1L, FormaPagamento.PIX, "TOKEN")
        );

        verify(pagamentos, never()).save(any(Pagamento.class));
        verify(pedidos, never()).save(any(Pedido.class));
        verify(notificacao, never()).notificarPagamento(any(), any());
        verify(entrega, never()).criarPara(any());
    }
}
