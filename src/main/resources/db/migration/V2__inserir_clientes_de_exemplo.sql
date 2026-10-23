INSERT INTO tb_clientes (id, nome, email) VALUES
(UNHEX(REPLACE('11111111-1111-1111-1111-111111111111', '-', '')), 'Ana Exemplo', 'ana@example.com'),
(UNHEX(REPLACE('22222222-2222-2222-2222-222222222222', '-', '')), 'Bruno Exemplo', 'bruno@example.com');
