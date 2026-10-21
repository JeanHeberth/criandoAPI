ALTER TABLE pedidos ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE pedidos ADD COLUMN idempotency_key_hash VARCHAR(64);
ALTER TABLE pedidos ADD COLUMN request_hash VARCHAR(64);
ALTER TABLE pedidos ADD CONSTRAINT uk_pedido_usuario_idempotencia UNIQUE (usuario_id, idempotency_key_hash);
CREATE INDEX idx_pedido_usuario_status_criado ON pedidos (usuario_id, status, criado_em);
CREATE INDEX idx_produto_ativo_categoria ON produtos (ativo, categoria);
