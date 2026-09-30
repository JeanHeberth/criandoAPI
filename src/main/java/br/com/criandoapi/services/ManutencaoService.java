package br.com.criandoapi.services;

import br.com.criandoapi.repository.PedidoRepository;
import br.com.criandoapi.repository.ProdutoRepository;
import br.com.criandoapi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManutencaoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public ManutencaoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void limparBanco() {
        // Pedidos primeiro: o cascade (CascadeType.ALL + orphanRemoval em Pedido.itens)
        // remove os itens_pedido junto. So depois disso produtos e usuarios podem ser
        // removidos sem violar as FKs de itens_pedido/pedidos.
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
}
