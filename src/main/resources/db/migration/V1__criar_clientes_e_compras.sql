CREATE TABLE tb_clientes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL
);

CREATE TABLE tb_compras (
    id BINARY(16) NOT NULL PRIMARY KEY,
    valor_usd DECIMAL(14,2) NOT NULL,
    cotacao_utilizada DECIMAL(30,15) NOT NULL,
    valor_convertido DECIMAL(19,2) NOT NULL,
    taxa_servico DECIMAL(19,2) NOT NULL,
    tributo_simulado DECIMAL(19,2) NOT NULL,
    valor_final_brl DECIMAL(19,2) NOT NULL,
    data_cotacao DATE NOT NULL,
    data_hora_cotacao DATETIME(6) NOT NULL,
    data_compra DATETIME(6) NOT NULL,
    cliente_id BINARY(16) NOT NULL,
    CONSTRAINT fk_compra_cliente FOREIGN KEY (cliente_id) REFERENCES tb_clientes(id),
    CONSTRAINT ck_compra_valor_usd CHECK (valor_usd > 0),
    CONSTRAINT ck_compra_cotacao CHECK (cotacao_utilizada > 0),
    INDEX ix_compra_cliente_data (cliente_id, data_compra)
);
