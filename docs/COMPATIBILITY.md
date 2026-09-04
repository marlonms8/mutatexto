# Compatibilidade Collura Decrypter v1.0 (Modo Legado)

Este documento registra os parâmetros do **Modo Legado** do MutaTexto
(antigo SINAPSE Crypto), usado exclusivamente para interoperar com o
formato de mensagens antigo. Para criptografia nova, use o Modo Moderno
— ver [MODERN_CRYPTO.md](MODERN_CRYPTO.md).

## Pipeline

```text
Senha UTF-8
   ↓
SHA-1
   ↓
Base64 sem '=' + LF final
   ↓
PBKDF2WithHmacSHA1
  salt = "Salt"
  iterations = 1
  length = 128 bits
   ↓
AES-128-CBC / PKCS5Padding
  IV = 00 00 00 00 00 00 00 00
       00 00 00 00 00 00 00 00
   ↓
Base64 do ciphertext
```

## Vetores de referência

```text
texto: teste
senha: 123456
saída: aMS10Jp7EWXiOT+FcQxGRw==
```

```text
texto: teste1
senha: 123456
saída: vgZ2fqx1dsnYp7/+rQdJ5w==
```

```text
texto: teste
senha: 654321
saída: PHnyQlWpUWo0T4ktBAQqEg==
```

Qualquer alteração futura na rotina criptográfica deve preservar esses vetores caso a compatibilidade legada continue sendo um requisito.
