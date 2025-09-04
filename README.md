# DevJails

![DevJails Logo](https://img.shields.io/badge/DevJails-v1.0.0-blue.svg)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.18.1--1.21.8+-green.svg)
![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 📋 Descrição

**DevJails** é um sistema completo e avançado de prisões para servidores Minecraft, desenvolvido para Bukkit/Spigot/Paper. O plugin oferece funcionalidades robustas para gerenciamento de prisioneiros, incluindo suporte a múltiplas cadeias, sistema de fiança, restrições personalizáveis e integração com plugins populares.

### ✨ Principais Funcionalidades

- 🏢 **Sistema de Múltiplas Cadeias**: Crie e gerencie várias cadeias em diferentes mundos
- ⏰ **Prisões Temporárias e Permanentes**: Flexibilidade total no tempo de prisão
- 💰 **Sistema de Fiança**: Permita que jogadores paguem para sair da prisão
- 🔗 **Algemas**: Sistema de restrição de movimento para prisioneiros
- 🗺️ **Áreas Personalizadas**: Defina zonas específicas com restrições customizadas
- 🔧 **Integração com WorldEdit/WorldGuard**: Suporte completo para seleção de áreas
- 💳 **Integração com Vault**: Sistema econômico para fianças
- 🌐 **Suporte Multilíngue**: Português, Inglês, Espanhol, Francês e Polonês
- 📊 **Interface Gráfica**: GUIs intuitivas para gerenciamento
- 🔄 **Sistema de Backup**: Backup automático dos dados
- 📈 **Métricas e Performance**: Otimizado para servidores de grande porte

## 🔧 Pré-requisitos

### Requisitos Obrigatórios
- **Minecraft Server**: Bukkit/Spigot/Paper 1.18.1 ou superior
- **Java**: Versão 17 ou superior
- **Vault**: Plugin obrigatório para sistema econômico

### Dependências Opcionais
- **WorldEdit**: Para seleção avançada de áreas
- **WorldGuard**: Para integração com regiões existentes

### Versões Testadas
- ✅ Paper 1.18.1 - 1.21.8+
- ✅ Spigot 1.18.1 - 1.21.8+
- ✅ Bukkit 1.18.1 - 1.21.8+

## 📦 Instalação

### Método 1: Download Direto
1. Baixe a versão mais recente do plugin na seção [Releases](https://github.com/devjails/devjails/releases)
2. Coloque o arquivo `DevJails-1.0.0.jar` na pasta `plugins/` do seu servidor
3. Instale o plugin **Vault** se ainda não estiver instalado
4. Reinicie o servidor

### Método 2: Compilação Manual
```bash
# Clone o repositório
git clone https://github.com/devjails/devjails.git
cd devjails

# Compile o projeto
./gradlew shadowJar

# O arquivo compilado estará em build/libs/
```

## ⚙️ Configuração

### Configuração Inicial

1. **Primeira Execução**: Após instalar o plugin, reinicie o servidor para gerar os arquivos de configuração

2. **Arquivo Principal** (`config.yml`):
```yaml
# Configuração do banco de dados
storage:
  type: "sqlite"  # ou "yaml"
  
# Configurações de economia
economy:
  enabled: true
  
# Sistema de fiança
bail:
  enabled: true
  max-amount: 100000
```

3. **Configuração de Mensagens**: Edite os arquivos em `plugins/DevJails/messages/` para personalizar as mensagens

### Configuração Avançada

#### Sistema de Armazenamento
```yaml
storage:
  type: "sqlite"  # Recomendado para a maioria dos servidores
  # type: "yaml"   # Para servidores menores
```

#### Integração com Discord
```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."
  events:
    jail: true
    unjail: true
    escape: true
```

#### Configurações de Performance
```yaml
performance:
  use-area-cache: true
  area-cache-size: 100
  max-prisoners-per-page: 45
```

## 🎮 Exemplos de Uso

### Comandos Básicos

#### Criando uma Cadeia
```
/djails setjail prisao_central
```

#### Prendendo um Jogador
```
# Prisão permanente
/djails jail Steve prisao_central Griefing

# Prisão temporária
/djails tempjail Steve prisao_central 2h Spam no chat
```

#### Sistema de Fiança
```
# Definir fiança
/djails bail set Steve 5000

# Abrir GUI de fianças
/djails bail gui
```

#### Gerenciamento de Áreas
```
# Criar área personalizada
/djails wand
# (selecione a área com a varinha)
/djails setflag area_restrita

# Vincular cadeia à área
/djails link prisao_central area_restrita
```

### Permissões Principais

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `djails.admin` | Acesso completo ao sistema | OP |
| `djails.jail` | Prender jogadores | OP |
| `djails.unjail` | Soltar jogadores | OP |
| `djails.bail.gui` | Acessar GUI de fianças | Todos |
| `djails.list` | Listar cadeias/prisioneiros | Todos |

### Exemplos de Cenários

#### Cenário 1: Servidor de Roleplay
```yaml
# Configuração para RP
restrictions:
  block-break: true
  block-place: true
  pvp-enabled: false
  chat-blocked: false
  
bail:
  enabled: true
  default-amount: 1000
```

#### Cenário 2: Servidor PvP
```yaml
# Configuração para PvP
restrictions:
  pvp-enabled: true
  commands-blocked: ["home", "spawn", "tp"]
  
escape:
  punishment-type: "extend"
  extend-time: "30m"
```

## 🤝 Contribuição

### Como Contribuir

1. **Fork** o repositório
2. Crie uma **branch** para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. **Commit** suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. **Push** para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um **Pull Request**

### Diretrizes de Desenvolvimento

#### Padrões de Código
- Siga as convenções Java padrão
- Use nomes descritivos para variáveis e métodos
- Adicione comentários em inglês para código complexo
- Mantenha métodos com no máximo 30 linhas

#### Estrutura de Commits
```
tipo(escopo): descrição breve

Descrição detalhada do que foi alterado e por quê.

Fixes #123
```

#### Tipos de Commit
- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação de código
- `refactor`: Refatoração
- `test`: Testes
- `chore`: Tarefas de manutenção

### Reportando Bugs

Ao reportar bugs, inclua:
- Versão do plugin
- Versão do servidor (Paper/Spigot/Bukkit)
- Versão do Java
- Logs de erro completos
- Passos para reproduzir o problema

### Sugestões de Funcionalidades

Para sugerir novas funcionalidades:
1. Verifique se já não existe uma issue similar
2. Descreva detalhadamente a funcionalidade
3. Explique o caso de uso
4. Considere a compatibilidade com versões existentes

## 📄 Licença

Este projeto está licenciado sob a **Licença MIT** - veja o arquivo [LICENSE](LICENSE) para detalhes.

```
MIT License

Copyright (c) 2024 DevJails Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORs OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 Suporte

- **GitHub Issues**: [Reportar problemas](https://github.com/devjails/devjails/issues)
- **Documentação**: [Wiki do projeto](https://github.com/devjails/devjails/wiki)
- **Discord**: [Servidor da comunidade](https://discord.gg/devjails)

## 🙏 Agradecimentos

- Equipe do **Paper** pelo excelente trabalho na API
- Desenvolvedores do **WorldEdit** e **WorldGuard** pela integração
- Comunidade **Bukkit/Spigot** pelo suporte contínuo
- Todos os contribuidores que ajudaram a melhorar o projeto

---

**Desenvolvido com ❤️ pela equipe DevJails**