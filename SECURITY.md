# Política de segurança

## Escopo

O MutaTexto oferece dois esquemas de criptografia de texto:

- **Modo Moderno (padrão)**: AES-256-GCM, PBKDF2-HMAC-SHA256 com 310.000
  iterações, salt e IV aleatórios por operação. Recomendado para todo uso novo.
- **Modo Legado**: mantido apenas para interoperabilidade com o formato
  antigo do Collura Decrypter v1.0 / SINAPSE Crypto 1.x. Usa parâmetros
  deliberadamente preservados (SHA-1, PBKDF2 com uma iteração, IV fixo) que
  **não devem ser considerados um padrão criptográfico moderno**. Use o
  Modo Legado somente para ler ou gerar mensagens que precisem ser
  compatíveis com esse formato antigo.

Em ambos os modos, a segurança prática depende fortemente da força da chave
escolhida pelo usuário — o app não impõe requisitos mínimos de complexidade
de chave.

## Dados

O aplicativo:

- não solicita permissão `INTERNET`;
- não envia textos ou chaves para servidores;
- não grava automaticamente o conteúdo criptografado, descriptografado ou a chave;
- processa as operações localmente no dispositivo.

## Relato de vulnerabilidades

Para problemas específicos do código do MutaTexto, utilize um issue no repositório e evite publicar textos, chaves ou outros dados reais usados na aplicação.
