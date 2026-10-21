CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE produtos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(500),
    preco DECIMAL(10,2) NOT NULL,
    estoque INT NOT NULL,
    categoria ENUM('ELETRONICO','VESTUARIO','ALIMENTACAO','LIVRO','ESPORTE','CASA_E_JARDIM','OUTRO') NOT NULL,
    ativo BOOLEAN NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    atualizado_em DATETIME(6)
);

CREATE TABLE pedidos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    status ENUM('PENDENTE','CONFIRMADO','EM_PREPARO','ENVIADO','ENTREGUE','CANCELADO') NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    atualizado_em DATETIME(6),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE itens_pedido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_item_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
