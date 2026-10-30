# 🌍 Estúdio Gerador de Mapas Voxel 3D (Java + OpenGL)

Um gerador e visualizador de terrenos tridimensionais em *voxels* desenvolvido em **Java** com foco extremo em **baixa utilização de CPU/RAM** e **alta performance gráfica**.

Projetado como um **Dashboard Técnico Interativo**, o sistema combina geração procedural em múltiplas camadas (montanhas, lagos, florestas, neve e cavernas subterrâneas 3D) com técnicas de otimização de motores gráficos modernos, incluindo **Face Culling** e **Bake & Disk Caching (`.BIN`)** direto no disco rígido.

## ✨ Principais Funcionalidades

* **🏔️ Geração Procedural Multimodal (`SimplexNoise`):**

  * **Superfície 2D com Oitavas Harmônicas:** Combina frequências macro e micro para criar cordilheiras naturais, colinas suaves e vales profundos.

  * **Escavação Volumétrica 3D:** Calcula densidade tridimensional no subsolo para esculpir redes de túneis e cavernas interconectadas.

* **🌲 Estratificação Automática de Biomas:**

  * **Praias e Lagos:** Planícies abaixo do nível da água são preenchidas com água translúcida e margens de areia.

  * **Florestas Voxelizadas:** Plantio determinístico de árvores (tronco de madeira e copa de folhas) nas encostas de grama.

  * **Picos Nevados:** Transição automática para rocha fria e cobertura de neve em altitudes elevadas ($Y \ge 24$).

* **⚡ Engenharia de Performance ("HD Forte, CPU Poupada"):**

  * **Compactação em Vetor Primitivo 1D:** Cada bloco do mundo consome apenas **1 byte** de memória RAM (`byte[]`), evitando o *overhead* de orientação a objetos da JVM.

  * **Face Culling (Filtro de Visibilidade):** Descarta entre $85\%$ **e** $90\%$ das faces subterrâneas ocultas antes de enviar os dados para a placa de vídeo.

  * **Cache Binário Persistente (`.BIN`):** Salva a malha visível já processada no disco (`cache_mapa/`). Configurações já visitadas são carregadas do HD em $1\text{ a }2\text{ ms}$ com custo zero de cálculo matemático na CPU.

* **🎛️ Dashboard Responsivo e Câmera Orbital:**

  * **Telemetria em Tempo Real:** Cartões superiores indicando Voxels Sólidos, Faces Brutas, Faces Renderizadas (% de *Culling*) e tamanho/status do Cache em Disco.

  * **Controles Interativos:** Barras deslizantes (*sliders*) para ajustar a Amplitude das Montanhas, Nível da Água e Escavação de Cavernas em tempo real.

  * **Modo Raio-X (Corte Transversal):** Permite abrir ou fechar as bordas laterais do mapa para inspecionar as galerias subterrâneas.

## 📐 Destaques de Engenharia e Matemática

### 1. Mapeamento Espacial 3D para Vetor Linear 1D

Para evitar a fragmentação de memória de matrizes tridimensionais (`byte[][][]`) no Java, cada `Chunk` de dimensão $16 \times 48 \times 16$ é armazenado em um único array contínuo de $12.288\text{ bytes}$. O acesso a qualquer coordenada $(x, y, z)$ é resolvido em tempo constante $O(1)$ pela equação:

$$
\text{Índice} = x + (z \cdot 16) + (y \cdot 16 \cdot 16)
$$

### 2. Cálculo de Relevo e Biomas

A cota vertical da superfície ($Y_{\text{sup}}$) em cada coluna $(X, Z)$ do mundo é governada pela combinação de oitavas do `SimplexNoise` modulada pelo *slider* de amplitude ($A$):

$$
Y_{\text{sup}} = \left\lfloor 14 + \text{Noise}_{2D}(X, Z) \cdot (A \cdot 1.6) \right\rfloor
$$

### 3. Câmera

Para proporcionar uma navegação suave controlada pelo mouse sem distorções, a câmera orbita o ponto central do mapa $(32, 14, 32)$ convertendo os ângulos de rotação horizontal ($\theta = \text{yaw}$), inclinação vertical ($\phi = \text{pitch}$) e distância de zoom ($D$) em coordenadas cartesianas:

$$
X_{\text{cam}} = 32 + D \cdot \cos(\phi) \cdot \sin(\theta)
$$

$$
Y_{\text{cam}} = 14 + D \cdot \sin(\phi)
$$

$$
Z_{\text{cam}} = 32 + D \cdot \cos(\phi) \cdot \cos(\theta)
$$

## 🎮 Controles e Interação

| Ação / Controle | Como Utilizar | Resultado no Visualizador | 
| ----- | ----- | ----- | 
| **Girar o Mapa 3D** | Clique e arraste na área central | Orbita a câmera suavemente ao redor do terreno | 
| **Zoom (Aproximar/Afastar)** | Roda do mouse (*Scroll*) | Ajusta a distância focal em relação às montanhas e cavernas | 
| **Sliders Inferiores** | Clique e arraste as barras | Altera montanhas, inunda lagos ou escava cavernas em tempo real | 
| **Exibir Corte Transversal** | Clique no botão esquerdo inferior | Abre/fecha as paredes externas do mapa para revelar o subsolo | 
| **Limpar Cache** | Clique no botão laranja | Apaga todos os arquivos `.bin` da pasta `cache_mapa/` no HD | 
| **Nova Seed** | Clique no botão azul | Sorteia uma nova semente matemática e gera uma geografia inédita | 
| **Resetar Câmera** | Clique no botão direito inferior | Restaura o ângulo e o zoom padrão de visualização | 

