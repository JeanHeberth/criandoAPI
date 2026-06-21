package br.com.criandoapi.entity;

public enum PedidoStatus {
    PENDENTE,
    CONFIRMADO,
    EM_PREPARO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    public boolean podeTransicionarPara(PedidoStatus destino) {
        return switch (this) {
            case PENDENTE    -> destino == CONFIRMADO || destino == CANCELADO;
            case CONFIRMADO  -> destino == EM_PREPARO || destino == CANCELADO;
            case EM_PREPARO  -> destino == ENVIADO;
            case ENVIADO     -> destino == ENTREGUE;
            default          -> false; // ENTREGUE e CANCELADO são estados finais
        };
    }
}

