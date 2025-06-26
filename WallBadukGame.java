import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class WallBadukGame extends JFrame {
    private static final int BOARD_SIZE = 9;
    private static final int CELL_SIZE = 40;
    private static final int STONE_SIZE = 32;
    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE]; // 0: empty, 1: black, 2: white
    private boolean blackTurn = true;
    private JLabel statusLabel;

    public WallBadukGame() {
        setTitle("벽바둑 (데블스 플랜)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(BOARD_SIZE * CELL_SIZE + 40, BOARD_SIZE * CELL_SIZE + 80);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        BoardPanel boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("흑돌 차례");
        add(statusLabel, BorderLayout.SOUTH);
    }

    private class BoardPanel extends JPanel {
        public BoardPanel() {
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX() / CELL_SIZE;
                    int y = e.getY() / CELL_SIZE;
                    if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) return;
                    if (board[y][x] != 0) return;
                    board[y][x] = blackTurn ? 1 : 2;
                    removeCapturedStones(x, y);
                    blackTurn = !blackTurn;
                    statusLabel.setText(blackTurn ? "흑돌 차례" : "백돌 차례");
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // 바둑판 그리기
            g.setColor(new Color(222, 184, 135));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            for (int i = 0; i < BOARD_SIZE; i++) {
                g.drawLine(CELL_SIZE / 2, CELL_SIZE / 2 + i * CELL_SIZE, CELL_SIZE / 2 + (BOARD_SIZE - 1) * CELL_SIZE, CELL_SIZE / 2 + i * CELL_SIZE);
                g.drawLine(CELL_SIZE / 2 + i * CELL_SIZE, CELL_SIZE / 2, CELL_SIZE / 2 + i * CELL_SIZE, CELL_SIZE / 2 + (BOARD_SIZE - 1) * CELL_SIZE);
            }
            // 돌 그리기
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    if (board[y][x] != 0) {
                        if (board[y][x] == 1) g.setColor(Color.BLACK);
                        else g.setColor(Color.WHITE);
                        g.fillOval(CELL_SIZE / 2 + x * CELL_SIZE - STONE_SIZE / 2, CELL_SIZE / 2 + y * CELL_SIZE - STONE_SIZE / 2, STONE_SIZE, STONE_SIZE);
                        g.setColor(Color.BLACK);
                        g.drawOval(CELL_SIZE / 2 + x * CELL_SIZE - STONE_SIZE / 2, CELL_SIZE / 2 + y * CELL_SIZE - STONE_SIZE / 2, STONE_SIZE, STONE_SIZE);
                    }
                }
            }
        }
    }

    // 벽(왼쪽 변)에 붙은 돌은 잡히지 않음
    private void removeCapturedStones(int lastX, int lastY) {
        int opponent = blackTurn ? 2 : 1;
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (Math.abs(dx) + Math.abs(dy) != 1) continue;
                int nx = lastX + dx;
                int ny = lastY + dy;
                if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
                if (board[ny][nx] == opponent && !hasLiberty(nx, ny, opponent, visited, false)) {
                    removeGroup(nx, ny, opponent);
                }
            }
        }
        // 자기 돌이 잡힌 경우(자살수 방지)
        visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        if (!hasLiberty(lastX, lastY, board[lastY][lastX], visited, false)) {
            board[lastY][lastX] = 0;
        }
    }

    // 벽에 붙은 돌은 무조건 살아있음
    private boolean hasLiberty(int x, int y, int color, boolean[][] visited, boolean wallSafe) {
        if (x == 0) return true; // 벽에 붙은 돌은 무조건 살아있음
        visited[y][x] = true;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int d = 0; d < 4; d++) {
            int nx = x + dx[d];
            int ny = y + dy[d];
            if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
            if (board[ny][nx] == 0) return true;
            if (board[ny][nx] == color && !visited[ny][nx]) {
                if (hasLiberty(nx, ny, color, visited, wallSafe)) return true;
            }
        }
        return false;
    }

    private void removeGroup(int x, int y, int color) {
        board[y][x] = 0;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int d = 0; d < 4; d++) {
            int nx = x + dx[d];
            int ny = y + dy[d];
            if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
            if (board[ny][nx] == color) {
                removeGroup(nx, ny, color);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WallBadukGame game = new WallBadukGame();
            game.setVisible(true);
        });
    }
} 