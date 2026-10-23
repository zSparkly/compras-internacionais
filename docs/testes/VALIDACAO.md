# Validação do segundo exercício

Execução em **23/10/2026**, no Windows, com JDK 21 e **MySQL Community Server 8.4.11**.

## Suíte automatizada

Comando: `mvnw.cmd -Pintegracao verify`.

| Classe | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| CalculoServiceTest | 6 | 0 | 0 | 0 |
| CompraServiceTest | 7 | 0 | 0 | 0 |
| CotacaoServiceTest | 15 | 0 | 0 | 0 |
| ValidacaoTest | 6 | 0 | 0 | 0 |
| CompraApiIT | 32 | 0 | 0 | 0 |
| **Total** | **66** | **0** | **0** | **0** |

Resultado: **BUILD SUCCESS**. Os arquivos `.txt` desta pasta são os resumos reais
gerados por Surefire e Failsafe. Casos parametrizados contam como execuções individuais.

Os 32 testes de integração usam MySQL real, HTTP real na API e OpenFeign real para
um servidor HTTP local controlado. Esse servidor é somente infraestrutura de teste:
permite reproduzir as condições externas, não faz parte da aplicação de produção.
A aplicação normal consulta a AwesomeAPI.

Foram verificadas as regras RN01 a RN08, incluindo uso de `ask` em vez de `bid`,
faixas de taxa de serviço, resposta externa inválida, timeout, histórico sem
recálculo, mês-calendário, isolamento por cliente e limite sob concorrência.

No teste simultâneo, duas compras de US$ 1.000,00 a 5,40 somariam R$ 12.096,00.
O resultado foi uma resposta **201** e uma **422**: apenas uma compra persistida,
total mensal de **R$ 6.048,00**, sem ultrapassar o limite.

## Consulta e compra com o provedor real

Além da suíte controlada, a aplicação empacotada foi iniciada na porta 8081,
com a URL oficial da AwesomeAPI e uma base MySQL separada para demonstração.
O arquivo `awesomeapi-real.json` registra os retornos reais dos quatro endpoints.

Em 23/10/2026, às 19:47 no horário de São Paulo:

- AwesomeAPI retornou `ask = 4.966`, com `create_date = 2026-10-23 18:30:08`.
- POST /compras com US$ 200,00: **201**, convertido R$ 993,20, taxa R$ 19,86,
  tributo R$ 99,32, valor final **R$ 1.112,38**.
- POST /compras/simular com US$ 99,99: **200**, convertido R$ 496,55,
  taxa R$ 24,83, tributo R$ 49,66, valor final **R$ 571,04**.
- Histórico e limite: **200**, refletindo apenas a compra registrada; a simulação
  não criou compra adicional.

Os valores de cotação variam. Esses resultados são evidências de uma execução,
não valores fixos esperados nas próximas consultas.
