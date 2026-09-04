# Modo Moderno (AES-256-GCM)

Esquema padrão do MutaTexto para criptografia nova. Diferente do Modo Legado,
não existe nenhum valor fixo: salt e IV são gerados aleatoriamente a cada
operação, então duas criptografias do mesmo texto com a mesma chave nunca
produzem o mesmo resultado.

## Pipeline

```text
Senha (UTF-8, caracteres)
   ↓
PBKDF2WithHmacSHA256
  salt = 16 bytes aleatórios (SecureRandom, por operação)
  iterations = 310.000
  length = 256 bits
   ↓
AES-256-GCM
  IV/nonce = 12 bytes aleatórios (SecureRandom, por operação)
  tag = 128 bits (autenticação integrada)
   ↓
payload = salt (16 bytes) || IV (12 bytes) || ciphertext+tag
   ↓
Base64 do payload
```

## Por que essa escolha

- **AES-256-GCM**: cifra autenticada (AEAD). Além de confidencialidade,
  detecta qualquer adulteração do texto cifrado ou uso de chave incorreta —
  a operação falha explicitamente em vez de produzir um texto corrompido
  silenciosamente, como acontece em modos sem autenticação (ex.: CBC puro).
- **PBKDF2-HMAC-SHA256, 310.000 iterações**: alinhado à recomendação da
  OWASP (Password Storage Cheat Sheet, revisão 2023) para PBKDF2-SHA256,
  tornando ataques de força bruta contra a chave significativamente mais
  custosos do que uma única iteração.
- **Salt e IV aleatórios por mensagem**: eliminam os dois problemas centrais
  do esquema legado (reuso de IV e ausência de salt), que permitiam
  correlacionar mensagens iguais cifradas com a mesma chave.

## Compatibilidade

Mensagens do Modo Moderno **não são compatíveis** com o Modo Legado
(Collura) nem com nenhuma versão anterior do SINAPSE Crypto. O formato de
saída inclui salt e IV no próprio payload, então basta a chave correta para
descriptografar — não é necessário armazenar ou transmitir salt/IV
separadamente.

## Nota sobre "AES-256"

O rótulo "AES-256" refere-se ao tamanho da chave derivada (256 bits), não a
alguma variante especial do algoritmo. A segurança prática do esquema
depende, na mesma medida, da força da senha escolhida pelo usuário — chaves
curtas ou previsíveis continuam sendo o elo mais fraco, independentemente do
tamanho da chave AES.
