package gerador;
import gerador.storage.DiskCache;
import gerador.world.BlockType;
import gerador.world.Chunk;
import gerador.world.SimplexNoise;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.raylib.Jaylib.Vector3;
import com.raylib.Jaylib.Color;
import com.raylib.Jaylib.Rectangle;
import com.raylib.Raylib.Vector2;
import static com.raylib.Jaylib.*;
import static com.raylib.Raylib.*;

public class Main {

    // Parâmetros controlados pelos Sliders
    private static int amplitudeMontanhas = 7;     // Escala: 1 a 14
    private static int nivelAgua = 12;             // Escala: 2 a 24
    private static float limiarCaverna = 0.35f;    // Escala: 0.00 a 0.65
    private static boolean corteTransversal = true;
    private static long seedAtual = 12345L;

    // Variáveis da Câmera
    private static float camYaw = 0.78f;
    private static float camPitch = 0.55f;
    private static float camDistance = 95.0f;

    // Métricas calculadas para os cartões superiores
    private static int totalVoxelsSolidos = 0;
    private static int totalFacesBrutas = 0;
    private static int totalFacesRenderizadas = 0;
    private static int percentualCulling = 0;
    private static float tamanhoCacheKB = 0.0f;
    private static String statusCache = "Formato .BIN";
    private static boolean veioDoDisco = false;

    public static void main(String[] args) {
        SetConfigFlags(FLAG_WINDOW_RESIZABLE);

        InitWindow(1020, 680, "Estudio Gerador de Mapas 3D - Voxel Dashboard");
        SetWindowPosition(80, 45);
        SetWindowMinSize(850, 560);
        SetTargetFPS(60);

        List<Chunk> worldChunks = new ArrayList<>();
        int mapRadius = 4; // Mapa 4x4 Chunks (64x64 blocos)
        for (int cx = 0; cx < mapRadius; cx++) {
            for (int cz = 0; cz < mapRadius; cz++) {
                worldChunks.add(new Chunk(cx, cz));
            }
        }

        regenerarMapa(worldChunks, mapRadius);

        Camera3D camera = new Camera3D()
                ._position(new Vector3())
                .target(new Vector3().x(32.0f).y(14.0f).z(32.0f))
                .up(new Vector3().x(0.0f).y(1.0f).z(0.0f))
                .fovy(45.0f)
                .projection(CAMERA_PERSPECTIVE);

        // Paleta de Cores 
        Color corFundoJanela = new Color(11, 13, 16, 255);
        Color corPainel      = new Color(20, 23, 28, 255);
        Color corBordaPainel = new Color(42, 47, 55, 255);
        Color corTextoBranco = new Color(240, 244, 248, 255);
        Color corTextoCinza  = new Color(145, 155, 168, 255);
        Color corVerdeDest   = new Color(46, 213, 115, 255);
        Color corAzulDest    = new Color(56, 161, 243, 255);
        Color corLaranjaDest = new Color(255, 168, 1, 255);

        // Cores dos blocos 3D 
        Color corGrama   = new Color(46, 196, 108, 255);
        Color corTerra   = new Color(138, 90, 68, 255);
        Color corPedra   = new Color(95, 105, 118, 255);
        Color corAreia   = new Color(230, 204, 165, 255);
        Color corAgua    = new Color(45, 135, 215, 175);
        Color corNeve    = new Color(245, 250, 255, 255);
        Color corMadeira = new Color(105, 68, 42, 255);
        Color corFolhas  = new Color(32, 148, 72, 255);
        Color corFio     = new Color(30, 35, 42, 110);

        Vector3 posAux = new Vector3();
        int sliderAtivo = 0;

        while (!WindowShouldClose()) {
            int W = GetScreenWidth();
            int H = GetScreenHeight();

            int margem = 24;
            int painelInfH = 190;
            int painelInfY = H - painelInfH - 14;
            int viewY = 100;
            int viewH = painelInfY - viewY - 12;

            int sliderX = 290;
            int sliderW = W - sliderX - 110;
            int slider1Y = painelInfY + 22;
            int slider2Y = painelInfY + 64;
            int slider3Y = painelInfY + 106;
            int botoesY  = painelInfY + 142;

            Vector2 mousePos = GetMousePosition();
            boolean mouseClicado = IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
            boolean mouseSegurado = IsMouseButtonDown(MOUSE_BUTTON_LEFT);

            if (!mouseSegurado) {
                sliderAtivo = 0;
            }

            boolean mouseNaArea3D = (mousePos.x() > margem && mousePos.x() < W - margem
                                  && mousePos.y() > viewY && mousePos.y() < viewY + viewH);

            // 1. Controle da camera
            if (sliderAtivo == 0 && mouseNaArea3D && (IsMouseButtonDown(MOUSE_BUTTON_LEFT) || IsMouseButtonDown(MOUSE_BUTTON_RIGHT))) {
                Vector2 delta = GetMouseDelta();
                camYaw   -= delta.x() * 0.005f;
                camPitch += delta.y() * 0.005f;

                if (camPitch < 0.08f) camPitch = 0.08f;
                if (camPitch > 1.48f) camPitch = 1.48f;
            }

            if (mouseNaArea3D) {
                float wheel = GetMouseWheelMove();
                if (wheel != 0) {
                    camDistance -= wheel * 5.0f;
                    if (camDistance < 25.0f) camDistance = 25.0f;
                    if (camDistance > 180.0f) camDistance = 180.0f;
                }
            }

            float camX = 32.0f + (float) (camDistance * Math.cos(camPitch) * Math.sin(camYaw));
            float camY = 14.0f + (float) (camDistance * Math.sin(camPitch));
            float camZ = 32.0f + (float) (camDistance * Math.cos(camPitch) * Math.cos(camYaw));
            camera._position(posAux.x(camX).y(camY).z(camZ));

            // 2. Interação com os Sliders e Botões
            boolean precisaRegenerar = false;

            if ((mouseClicado && naAreaSlider(mousePos, sliderX, slider1Y, sliderW)) || sliderAtivo == 1) {
                sliderAtivo = 1;
                int novoValor = Math.round(lerProporcaoSlider(mousePos.x(), sliderX, sliderW) * 13) + 1;
                if (novoValor != amplitudeMontanhas) {
                    amplitudeMontanhas = novoValor;
                    precisaRegenerar = true;
                }
            }

            if ((mouseClicado && naAreaSlider(mousePos, sliderX, slider2Y, sliderW)) || sliderAtivo == 2) {
                sliderAtivo = 2;
                int novoValor = Math.round(lerProporcaoSlider(mousePos.x(), sliderX, sliderW) * 22) + 2;
                if (novoValor != nivelAgua) {
                    nivelAgua = novoValor;
                    precisaRegenerar = true;
                }
            }

            if ((mouseClicado && naAreaSlider(mousePos, sliderX, slider3Y, sliderW)) || sliderAtivo == 3) {
                sliderAtivo = 3;
                float novoValor = Math.round(lerProporcaoSlider(mousePos.x(), sliderX, sliderW) * 65.0f) / 100.0f;
                if (Math.abs(novoValor - limiarCaverna) > 0.005f) {
                    limiarCaverna = novoValor;
                    precisaRegenerar = true;
                }
            }

            // Botão Corte Transversal
            if (mouseClicado && dentroRetangulo(mousePos, margem + 20, botoesY, 355, 34)) {
                corteTransversal = !corteTransversal;
                precisaRegenerar = true;
            }

            // Botão Limpar Cache HD
            if (mouseClicado && dentroRetangulo(mousePos, W - margem - 495, botoesY, 145, 34)) {
                DiskCache.limparTodoCache();
                precisaRegenerar = true;
            }

            // Botão Nova Seed
            if (mouseClicado && dentroRetangulo(mousePos, W - margem - 335, botoesY, 145, 34)) {
                seedAtual = System.currentTimeMillis() % 100000;
                precisaRegenerar = true;
            }

            // Botão Resetar Câmera
            if (mouseClicado && dentroRetangulo(mousePos, W - margem - 175, botoesY, 155, 34)) {
                camYaw = 0.78f;
                camPitch = 0.55f;
                camDistance = 95.0f;
            }

            if (precisaRegenerar) {
                regenerarMapa(worldChunks, mapRadius);
            }

            // Desenho da tela
            BeginDrawing();
            ClearBackground(corFundoJanela);

            desenharCaixaComBorda(margem, viewY, W - (margem * 2), viewH, corPainel, corBordaPainel);

            BeginScissorMode(margem + 2, viewY + 2, W - (margem * 2) - 4, viewH - 4);
            BeginMode3D(camera);
            for (Chunk chunk : worldChunks) {
                int baseX = chunk.chunkX * Chunk.SIZE_X;
                int baseZ = chunk.chunkZ * Chunk.SIZE_Z;

                for (int[] b : chunk.visibleBlocks) {
                    posAux.x(baseX + b[0]).y(b[1]).z(baseZ + b[2]);
                    byte type = (byte) b[3];

                    Color c = switch (type) {
                        case BlockType.GRASS  -> corGrama;
                        case BlockType.DIRT   -> corTerra;
                        case BlockType.STONE  -> corPedra;
                        case BlockType.SAND   -> corAreia;
                        case BlockType.SNOW   -> corNeve;
                        case BlockType.WOOD   -> corMadeira;
                        case BlockType.LEAVES -> corFolhas;
                        default               -> corAgua;
                    };

                    DrawCube(posAux, 1.0f, 1.0f, 1.0f, c);
                    if (type != BlockType.WATER) {
                        DrawCubeWires(posAux, 1.0f, 1.0f, 1.0f, corFio);
                    }
                }
            }
            posAux.x(31.5f).y(23.5f).z(31.5f);
            DrawCubeWires(posAux, 64.0f, 48.0f, 64.0f, corBordaPainel);
            EndMode3D();
            EndScissorMode();

            desenharCaixaComBorda(margem + 16, viewY + 12, 330, 30, corFundoJanela, corBordaPainel);
            DrawCircle(margem + 32, viewY + 27, 5, corAzulDest);
            DrawText(corteTransversal ? "Mapa 64x64  -  Corte Transversal Ativo" : "Mapa 64x64  -  Bordas Fechadas",
                    margem + 46, viewY + 20, 14, corTextoBranco);

            // 4 Cartões Superiores Responsivos
            int espacoCartoes = 14;
            int cardW = (W - (margem * 2) - (espacoCartoes * 3)) / 4;
            int cardY = 12;
            int cardH = 76;

            desenharCartaoTopo(margem, cardY, cardW, cardH, "Voxels Solidos",
                    String.format(Locale.US, "%,d", totalVoxelsSolidos).replace(',', '.'),
                    "Grid 64x48x64 (4x4 Chunks)", corVerdeDest, corPainel, corBordaPainel, corTextoBranco, corTextoCinza);

            desenharCartaoTopo(margem + (cardW + espacoCartoes), cardY, cardW, cardH, "Faces Brutas",
                    String.format(Locale.US, "%,d", totalFacesBrutas).replace(',', '.'),
                    "Sem otimizacao", corTextoCinza, corPainel, corBordaPainel, corTextoBranco, corTextoCinza);

            desenharCartaoTopo(margem + (cardW + espacoCartoes) * 2, cardY, cardW, cardH, "Faces Renderizadas",
                    String.format(Locale.US, "%,d", totalFacesRenderizadas).replace(',', '.'),
                    "-" + percentualCulling + "% Culling", corVerdeDest, corPainel, corBordaPainel, corTextoBranco, corTextoCinza);

            desenharCartaoTopo(margem + (cardW + espacoCartoes) * 3, cardY, cardW, cardH, "Cache em Disco",
                    String.format(Locale.US, "%.1f KB", tamanhoCacheKB),
                    statusCache, veioDoDisco ? corVerdeDest : corAzulDest, corPainel, corBordaPainel, corTextoBranco, corTextoCinza);

            // Painel Inferior de Controle Responsivo
            desenharCaixaComBorda(margem, painelInfY, W - (margem * 2), painelInfH, corPainel, corBordaPainel);

            desenharSlider(margem + 20, slider1Y, sliderX, sliderW, "Amplitude das Montanhas",
                    (amplitudeMontanhas - 1) / 13.0f, String.valueOf(amplitudeMontanhas),
                    corTextoBranco, corTextoCinza, corBordaPainel);

            desenharSlider(margem + 20, slider2Y, sliderX, sliderW, "Nivel da Agua / Lagos",
                    (nivelAgua - 2) / 22.0f, String.valueOf(nivelAgua),
                    corTextoBranco, corTextoCinza, corBordaPainel);

            desenharSlider(margem + 20, slider3Y, sliderX, sliderW, "Escavacao de Cavernas 3D",
                    limiarCaverna / 0.65f, String.format(Locale.US, "%.2f", limiarCaverna),
                    corTextoBranco, corTextoCinza, corBordaPainel);

            desenharCaixaComBorda(margem + 20, botoesY, 355, 34, corFundoJanela, corBordaPainel);
            DrawCircle(margem + 38, botoesY + 17, 6, corteTransversal ? corVerdeDest : corBordaPainel);
            DrawText("Exibir Corte Transversal (Revelar Interior)", margem + 52, botoesY + 9, 14, corTextoBranco);

            desenharCaixaComBorda(W - margem - 495, botoesY, 145, 34, corFundoJanela, corBordaPainel);
            DrawText("Limpar Cache", W - margem - 472, botoesY + 9, 14, corLaranjaDest);

            desenharCaixaComBorda(W - margem - 335, botoesY, 145, 34, corFundoJanela, corBordaPainel);
            DrawText("Nova Seed", W - margem - 302, botoesY + 9, 15, corAzulDest);

            desenharCaixaComBorda(W - margem - 175, botoesY, 155, 34, corFundoJanela, corBordaPainel);
            DrawText("Resetar Camera", W - margem - 155, botoesY + 9, 15, corTextoBranco);

            EndDrawing();
        }

        CloseWindow();
    }

    // Integração com o cache em disco
    private static void regenerarMapa(List<Chunk> chunks, int mapRadius) {
        long inicio = System.currentTimeMillis();
        long bytesNoDisco;

        // 1. Verifica se essa combinação já existe salva no HD
        if (DiskCache.existeNoCache(seedAtual, amplitudeMontanhas, nivelAgua, limiarCaverna, corteTransversal, mapRadius)) {
            bytesNoDisco = DiskCache.carregarMapaDoDisco(chunks, seedAtual, amplitudeMontanhas, nivelAgua, limiarCaverna, corteTransversal);
            long tempoMs = System.currentTimeMillis() - inicio;
            statusCache = "Lido do HD (" + tempoMs + " ms)";
            veioDoDisco = true;
        } else {
            // 2. Se não existe, calcula na CPU e salva os arquivos .BIN no HD
            SimplexNoise noise = new SimplexNoise(seedAtual);
            for (Chunk c : chunks) {
                c.generateTerrain(noise, amplitudeMontanhas, nivelAgua, limiarCaverna, corteTransversal, mapRadius);
            }
            bytesNoDisco = DiskCache.salvarMapaNoDisco(chunks, seedAtual, amplitudeMontanhas, nivelAgua, limiarCaverna, corteTransversal);
            long tempoMs = System.currentTimeMillis() - inicio;
            statusCache = "Gravado .BIN (" + tempoMs + " ms)";
            veioDoDisco = false;
        }

        totalVoxelsSolidos = 0;
        totalFacesRenderizadas = 0;
        for (Chunk c : chunks) {
            totalVoxelsSolidos += c.solidVoxelsCount;
            totalFacesRenderizadas += c.renderedFacesCount;
        }

        totalFacesBrutas = totalVoxelsSolidos * 6;
        if (totalFacesBrutas > 0) {
            percentualCulling = Math.round((1.0f - ((float) totalFacesRenderizadas / totalFacesBrutas)) * 100.0f);
        } else {
            percentualCulling = 0;
        }
        tamanhoCacheKB = bytesNoDisco / 1024.0f;
    }

    private static void desenharCaixaComBorda(int x, int y, int w, int h, Color fundo, Color borda) {
        DrawRectangleRounded(new Rectangle(x - 1, y - 1, w + 2, h + 2), 0.12f, 8, borda);
        DrawRectangleRounded(new Rectangle(x, y, w, h), 0.12f, 8, fundo);
    }

    private static void desenharCartaoTopo(int x, int y, int w, int h, String titulo, String valor, String subtitulo,
                                           Color corSub, Color fundo, Color borda, Color corValor, Color corTitulo) {
        desenharCaixaComBorda(x, y, w, h, fundo, borda);
        DrawText(titulo, x + 14, y + 8, 13, corTitulo);
        DrawText(valor, x + 14, y + 26, 22, corValor);
        DrawText(subtitulo, x + 14, y + 54, 13, corSub);
    }

    private static void desenharSlider(int xTexto, int y, int xBarra, int larguraBarra, String label,
                                       float proporcao, String valorTexto, Color corBranca, Color corCinza, Color corTrilha) {
        if (proporcao < 0f) proporcao = 0f;
        if (proporcao > 1f) proporcao = 1f;

        DrawText(label, xTexto, y - 4, 15, corCinza);
        DrawRectangle(xBarra, y + 3, larguraBarra, 4, corTrilha);

        int larguraPreenchida = (int) (larguraBarra * proporcao);
        DrawRectangle(xBarra, y + 3, larguraPreenchida, 4, corBranca);

        int pinoX = xBarra + larguraPreenchida;
        DrawCircle(pinoX, y + 5, 8, corBranca);
        DrawCircle(pinoX, y + 5, 3, corTrilha);

        DrawText(valorTexto, xBarra + larguraBarra + 25, y - 4, 17, corBranca);
    }

    private static boolean naAreaSlider(Vector2 mouse, int xBarra, int y, int larguraBarra) {
        return mouse.x() >= xBarra - 10 && mouse.x() <= xBarra + larguraBarra + 10
                && mouse.y() >= y - 12 && mouse.y() <= y + 20;
    }

    private static boolean dentroRetangulo(Vector2 mouse, int x, int y, int w, int h) {
        return mouse.x() >= x && mouse.x() <= x + w && mouse.y() >= y && mouse.y() <= y + h;
    }

    private static float lerProporcaoSlider(float mouseX, int xBarra, int larguraBarra) {
        float p = (mouseX - xBarra) / (float) larguraBarra;
        if (p < 0.0f) return 0.0f;
        if (p > 1.0f) return 1.0f;
        return p;
    }
}