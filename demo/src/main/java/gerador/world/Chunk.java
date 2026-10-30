package gerador.world;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 48;
    public static final int SIZE_Z = 16;
    public static final int SNOW_LEVEL = 24; // Acima de Y=24 vira Neve!

    public final int chunkX, chunkZ;
    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];

    // Guarda os blocos visíveis: {x, y, z, tipo}
    public final List<int[]> visibleBlocks = new ArrayList<>();

    // Métricas para os cartões do topo
    public int solidVoxelsCount = 0;
    public int renderedFacesCount = 0;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    private int getIndex(int x, int y, int z) {
        return x + (z * SIZE_X) + (y * SIZE_X * SIZE_Z);
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return BlockType.AIR;
        }
        return blocks[getIndex(x, y, z)];
    }

    public void setBlock(int x, int y, int z, byte type) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return;
        blocks[getIndex(x, y, z)] = type;
    }

    public void generateTerrain(SimplexNoise noise, int amplitude, int waterLevel, float caveThreshold, boolean corteTransversal, int mapRadius) {
        solidVoxelsCount = 0;

        // Limpa o chunk antes de regenerar
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = BlockType.AIR;
        }

        // Guarda a altura da superfície para plantar as arvores depois
        int[][] surfaceMap = new int[SIZE_X][SIZE_Z];

        // 1. gera o relevo, lagos e cavernas
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int worldX = chunkX * SIZE_X + x;
                int worldZ = chunkZ * SIZE_Z + z;

                double n = noise.getHeightNoise(worldX, worldZ);
                int surfaceY = (int) (14 + n * (amplitude * 1.6));
                if (surfaceY < 2) surfaceY = 2;
                if (surfaceY >= SIZE_Y - 8) surfaceY = SIZE_Y - 8; // Deixa espaço no topo para árvores
                surfaceMap[x][z] = surfaceY;

                for (int y = 0; y < SIZE_Y; y++) {
                    if (y == 0) {
                        setBlock(x, y, z, BlockType.STONE);
                        continue;
                    }

                    if (y <= surfaceY) {
                        double caveNoise = (noise.noise3D(worldX * 0.08, y * 0.10, worldZ * 0.08) + 1.0) * 0.5;

                        boolean naBordaDoMapa = (chunkX == 0 && x == 0) || (chunkZ == 0 && z == 0)
                                || (chunkX == mapRadius - 1 && x == SIZE_X - 1)
                                || (chunkZ == mapRadius - 1 && z == SIZE_Z - 1);

                        if (y < surfaceY - 1 && caveNoise < caveThreshold && (corteTransversal || !naBordaDoMapa)) {
                            setBlock(x, y, z, BlockType.AIR);
                        } else {
                            if (y == surfaceY) {
                                // Define o bioma do topo
                                if (y <= waterLevel + 1) {
                                    setBlock(x, y, z, BlockType.SAND);
                                } else if (y >= SNOW_LEVEL) {
                                    setBlock(x, y, z, BlockType.SNOW);
                                } else {
                                    setBlock(x, y, z, BlockType.GRASS);
                                }
                            } else if (y >= surfaceY - 3) {
                                setBlock(x, y, z, (y >= SNOW_LEVEL) ? BlockType.STONE : BlockType.DIRT);
                            } else {
                                setBlock(x, y, z, BlockType.STONE);
                            }
                        }
                    } else if (y <= waterLevel) {
                        setBlock(x, y, z, BlockType.WATER);
                    }
                }
            }
        }

        // 2. Gera arvores simples no topo de uma colina
        // Começa em 2 e vai até SIZE_X - 3 para a copa 3x3 não vazar para fora do Chunk
        for (int x = 2; x < SIZE_X - 2; x += 3) {
            for (int z = 2; z < SIZE_Z - 2; z += 3) {
                int yChao = surfaceMap[x][z];

                // Só gera a árvore se o chão for Grama 
                if (getBlock(x, yChao, z) == BlockType.GRASS) {
                    int worldX = chunkX * SIZE_X + x;
                    int worldZ = chunkZ * SIZE_Z + z;

                    // Usa o ruído em alta frequência para decidir onde gerar uma árvore
                    double treeChance = noise.noise3D(worldX * 0.65, 50.0, worldZ * 0.65);
                    if (treeChance > 0.32) {
                        placeTree(x, yChao + 1, z);
                    }
                }
            }
        }

        bakeVisibleBlocks();
    }

    // Gera uma arvore estilo Voxel 
    private void placeTree(int x, int yBase, int z) {
        if (yBase + 6 >= SIZE_Y) return;

        // Tronco de madeira 
        for (int h = 0; h < 4; h++) {
            setBlock(x, yBase + h, z, BlockType.WOOD);
        }

        // Camadas inferiores da copa 
        for (int ly = yBase + 2; ly <= yBase + 3; ly++) {
            for (int lx = -1; lx <= 1; lx++) {
                for (int lz = -1; lz <= 1; lz++) {
                    if (Math.abs(lx) == 1 && Math.abs(lz) == 1) continue; // Corta as quinas
                    if (getBlock(x + lx, ly, z + lz) == BlockType.AIR) {
                        setBlock(x + lx, ly, z + lz, BlockType.LEAVES);
                    }
                }
            }
        }

        // Topo da copa 
        setBlock(x, yBase + 4, z, BlockType.LEAVES);
        setBlock(x + 1, yBase + 4, z, BlockType.LEAVES);
        setBlock(x - 1, yBase + 4, z, BlockType.LEAVES);
        setBlock(x, yBase + 4, z + 1, BlockType.LEAVES);
        setBlock(x, yBase + 4, z - 1, BlockType.LEAVES);
    }

    public void bakeVisibleBlocks() {
        visibleBlocks.clear();
        renderedFacesCount = 0;
        solidVoxelsCount = 0;

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    byte type = getBlock(x, y, z);
                    if (type == BlockType.AIR) continue;

                    solidVoxelsCount++;

                    if (type == BlockType.WATER) {
                        if (getBlock(x, y + 1, z) == BlockType.AIR) {
                            visibleBlocks.add(new int[]{x, y, z, type});
                            renderedFacesCount++;
                        }
                        continue;
                    }

                    int exposedFaces = 0;
                    if (isTransparent(getBlock(x + 1, y, z))) exposedFaces++;
                    if (isTransparent(getBlock(x - 1, y, z))) exposedFaces++;
                    if (isTransparent(getBlock(x, y + 1, z))) exposedFaces++;
                    if (isTransparent(getBlock(x, y - 1, z))) exposedFaces++;
                    if (isTransparent(getBlock(x, y, z + 1))) exposedFaces++;
                    if (isTransparent(getBlock(x, y, z - 1))) exposedFaces++;

                    if (exposedFaces > 0) {
                        visibleBlocks.add(new int[]{x, y, z, type});
                        renderedFacesCount += exposedFaces;
                    }
                }
            }
        }
    }

    private boolean isTransparent(byte b) {
        return b == BlockType.AIR || b == BlockType.WATER;
    }

    public byte[] getRawBytes() { return blocks; }
}