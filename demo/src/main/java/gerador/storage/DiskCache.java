package gerador.storage;
import gerador.world.Chunk;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class DiskCache {

    private static final String PASTA_BASE = "cache_mapa";

    // Cria o nome da subpasta com base nas configurações atuais dos Sliders
    private static String obterPastaConfig(long seed, int amp, int water, float cave, boolean corte) {
        int caveInt = Math.round(cave * 100);
        int corteInt = corte ? 1 : 0;
        return PASTA_BASE + File.separator + String.format("s%d_a%d_w%d_c%d_t%d", seed, amp, water, caveInt, corteInt);
    }

    private static File obterArquivoChunk(String pastaConfig, int chunkX, int chunkZ) {
        return new File(pastaConfig, String.format("chunk_%d_%d.bin", chunkX, chunkZ));
    }

    // Verifica se todos os Chunks dessa configuração já estão salvos no HD
    public static boolean existeNoCache(long seed, int amp, int water, float cave, boolean corte, int mapRadius) {
        String pasta = obterPastaConfig(seed, amp, water, cave, corte);
        for (int cx = 0; cx < mapRadius; cx++) {
            for (int cz = 0; cz < mapRadius; cz++) {
                if (!obterArquivoChunk(pasta, cx, cz).exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Salva os chunks no hd
    public static long salvarMapaNoDisco(List<Chunk> chunks, long seed, int amp, int water, float cave, boolean corte) {
        String pasta = obterPastaConfig(seed, amp, water, cave, corte);
        File dir = new File(pasta);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long bytesTotaisGravados = 0;

        for (Chunk c : chunks) {
            File arquivo = obterArquivoChunk(pasta, c.chunkX, c.chunkZ);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(arquivo)))) {
                // 1. Grava as métricas pré-calculadas do Chunk 
                out.writeInt(c.solidVoxelsCount);
                out.writeInt(c.renderedFacesCount);

                // 2. Grava a quantidade de blocos visíveis 
                out.writeInt(c.visibleBlocks.size());

                // 3. Grava apenas os blocos visíveis mastigados 
                for (int[] b : c.visibleBlocks) {
                    out.writeByte((byte) b[0]);
                    out.writeByte((byte) b[1]);
                    out.writeByte((byte) b[2]);
                    out.writeByte((byte) b[3]);
                }
                out.flush();
            } catch (IOException e) {
                System.err.println("Erro ao salvar chunk no HD: " + e.getMessage());
            }
            bytesTotaisGravados += arquivo.length();
        }
        return bytesTotaisGravados;
    }

    // Carrega os chunks direto do hd
    public static long carregarMapaDoDisco(List<Chunk> chunks, long seed, int amp, int water, float cave, boolean corte) {
        String pasta = obterPastaConfig(seed, amp, water, cave, corte);
        long bytesTotaisLidos = 0;

        for (Chunk c : chunks) {
            File arquivo = obterArquivoChunk(pasta, c.chunkX, c.chunkZ);
            if (!arquivo.exists()) continue;

            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(arquivo)))) {
                // 1. Lê as métricas prontas
                c.solidVoxelsCount = in.readInt();
                c.renderedFacesCount = in.readInt();

                // 2. Lê a lista de blocos visíveis pronta para a placa de vídeo desenhar
                int qtdVisiveis = in.readInt();
                c.visibleBlocks.clear();

                for (int i = 0; i < qtdVisiveis; i++) {
                    int x = in.readByte() & 0xFF;
                    int y = in.readByte() & 0xFF;
                    int z = in.readByte() & 0xFF;
                    int type = in.readByte() & 0xFF;
                    c.visibleBlocks.add(new int[]{x, y, z, type});
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler chunk do HD: " + e.getMessage());
            }
            bytesTotaisLidos += arquivo.length();
        }
        return bytesTotaisLidos;
    }

    // Limpa a pasta de cache se o usuário clicar no botão de limpar
    public static void limparTodoCache() {
        File base = new File(PASTA_BASE);
        deletarRecursivo(base);
    }

    private static void deletarRecursivo(File f) {
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) {
                for (File filho : filhos) deletarRecursivo(filho);
            }
        }
        f.delete();
    }
}