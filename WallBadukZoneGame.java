import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class WallBadukZoneGame extends JFrame {
    private static final int BOARD_SIZE = 7;
    private static final int CELL_SIZE = 100;
    private static final int STONE_SIZE = 70;
    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE]; // 0: empty, 1: black, 2: white
    private boolean blackTurn = true;
    private Point selectedStone = null;
    private int[][] hWalls = new int[BOARD_SIZE+1][BOARD_SIZE]; // 0:없음, 1:플1, 2:플2
    private int[][] vWalls = new int[BOARD_SIZE][BOARD_SIZE+1]; // 0:없음, 1:플1, 2:플2
    private JLabel statusLabel;
    private boolean gameEnded = false;
    private int[] stonesPlaced = new int[5]; // [0]:dummy, [1]~[4]:플레이어별 돌 개수
    private boolean placingPhase = true;
    private int stonesPerPlayer = 2;
    private Point lastMovedStone = null;
    private boolean waitingForWall = false;
    private java.util.List<Point> movableSpots = new ArrayList<>(); // 이동 가능 위치 표시용
    private int placingCount = 0; // 초기 배치 순서 제어용
    private int playerCount = 2;
    private String[] playerNames = new String[5]; // 0~4, 1~playerCount 사용
    private Color[] playerColors = new Color[5]; // 0~4, 1~playerCount 사용
    private Color[] wallColors = new Color[5]; // 0~4, 1~playerCount 사용
    private int[] placingOrder; // 초기 배치 순서
    // 상태 저장용(되돌리기)
    private static class GameState {
        int[][] board;
        int[][] hWalls;
        int[][] vWalls;
        boolean blackTurn;
        boolean waitingForWall;
        Point lastMovedStone;
        boolean placingPhase;
        int placingCount;
        int[] stonesPlaced;
    }
    private Stack<GameState> history = new Stack<>();
    private boolean wallBreakMode = false;
    private int currentPlayer = 1; // 현재 차례(1~playerCount)

    public WallBadukZoneGame() {
        // 색상 지정 (1:노랑, 2:파랑, 3:빨강, 4:초록)
        playerColors[1] = Color.YELLOW;
        playerColors[2] = new Color(30, 100, 255);
        playerColors[3] = Color.RED;
        playerColors[4] = Color.GREEN;
        wallColors[1] = Color.YELLOW;
        wallColors[2] = new Color(30, 100, 255);
        wallColors[3] = Color.RED;
        wallColors[4] = Color.GREEN;
        // --- 플레이어 수 및 이름 입력 ---
        Object[] options = {"2인", "3인", "4인"};
        int selected = JOptionPane.showOptionDialog(null, "플레이어 수를 선택하세요", "벽바둑 시작", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (selected == 1) playerCount = 3;
        else if (selected == 2) playerCount = 4;
        else playerCount = 2;
        for (int i = 1; i <= playerCount; i++) {
            String name = JOptionPane.showInputDialog(null, i + "번 플레이어 이름을 입력하세요", "플레이어 이름", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) playerNames[i] = name.trim();
            else playerNames[i] = "플레이어" + i;
        }
        // --- 초기 배치 순서 배열 생성 ---
        if (playerCount == 2) placingOrder = new int[]{1,2,2,1};
        else if (playerCount == 3) placingOrder = new int[]{1,2,3,3,2,1};
        else placingOrder = new int[]{1,2,3,4,4,3,2,1};
        setTitle("벽바둑 (" + playerCount + "인전)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 1200);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 20));
        BoardPanel boardPanel = new BoardPanel();
        Dimension boardDim = new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
        boardPanel.setPreferredSize(boardDim);
        boardPanel.setMinimumSize(boardDim);
        boardPanel.setMaximumSize(boardDim);
        boardPanel.setFocusable(true);
        boardPanel.requestFocusInWindow();
        // 플레이어 정보 패널
        JPanel playerInfoPanel = new JPanel();
        playerInfoPanel.setLayout(new BoxLayout(playerInfoPanel, BoxLayout.Y_AXIS));
        playerInfoPanel.setBorder(BorderFactory.createTitledBorder("플레이어 정보"));
        String[] colorNames = {"", "노랑", "파랑", "빨강", "초록"};
        for (int i = 1; i <= playerCount; i++) {
            JLabel label = new JLabel(playerNames[i] + " : " + colorNames[i]);
            label.setFont(new Font("Dialog", Font.BOLD, 20));
            label.setForeground(playerColors[i]);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            playerInfoPanel.add(label);
            playerInfoPanel.add(Box.createVerticalStrut(10));
        }
        playerInfoPanel.setPreferredSize(new Dimension(200, BOARD_SIZE * CELL_SIZE));
        // 가로로 배치할 패널
        JPanel horizontalPanel = new JPanel();
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        horizontalPanel.setOpaque(false);
        JPanel leftSpacer = new JPanel();
        leftSpacer.setPreferredSize(new Dimension(40, 10));
        leftSpacer.setOpaque(false);
        leftSpacer.setEnabled(false);
        JPanel rightSpacer = new JPanel();
        rightSpacer.setPreferredSize(new Dimension(40, 10));
        rightSpacer.setOpaque(false);
        rightSpacer.setEnabled(false);
        horizontalPanel.add(leftSpacer);
        horizontalPanel.add(boardPanel);
        horizontalPanel.add(rightSpacer);
        horizontalPanel.add(playerInfoPanel);
        // 중앙 정렬
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(horizontalPanel);
        add(centerPanel, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        // 각 플레이어별 초기 배치 개수 계산
        int[] placeCount = new int[5];
        for (int v : placingOrder) placeCount[v]++;
        int nextColor = placingOrder[0];
        int num = 1;
        statusLabel = new JLabel(playerNames[nextColor] + ": 빈 칸에 말을 놓으세요 (1/" + placeCount[nextColor] + ")");
        statusLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(statusLabel);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 10));
        JButton undoBtn = new JButton("되돌리기");
        undoBtn.setPreferredSize(new Dimension(120, 40));
        undoBtn.setFont(new Font("Dialog", Font.PLAIN, 18));
        undoBtn.addActionListener(e -> undo());
        buttonPanel.add(undoBtn);
        JButton breakBtn = new JButton("벽 부수기");
        breakBtn.setPreferredSize(new Dimension(120, 40));
        breakBtn.setFont(new Font("Dialog", Font.PLAIN, 18));
        breakBtn.addActionListener(e -> {
            if (!wallBreakMode && !placingPhase && !waitingForWall && !gameEnded) {
                wallBreakMode = true;
                statusLabel.setText(playerNames[getCurrentPlayer()] + ": 벽 부수기 모드 (이동 경로에 벽 1개까지 무시 가능)");
            }
        });
        buttonPanel.add(breakBtn);
        JButton passBtn = new JButton("넘기기");
        passBtn.setPreferredSize(new Dimension(120, 40));
        passBtn.setFont(new Font("Dialog", Font.PLAIN, 18));
        passBtn.addActionListener(e -> {
            if (!placingPhase && !waitingForWall && !gameEnded) {
                nextTurn();
                selectedStone = null;
                movableSpots.clear();
                wallBreakMode = false;
                statusLabel.setText(playerNames[getCurrentPlayer()] + " 차례: 말을 선택하세요");
                repaint();
            }
        });
        buttonPanel.add(passBtn);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(buttonPanel);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private class BoardPanel extends JPanel {
        public BoardPanel() {
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (gameEnded) return;
                    int x = e.getX() / CELL_SIZE;
                    int y = e.getY() / CELL_SIZE;
                    if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) return;
                    if (placingPhase) {
                        int color = placingOrder[placingCount];
                        if (board[y][x] == 0) {
                            board[y][x] = color;
                            stonesPlaced[color]++;
                            placingCount++;
                            if (placingCount < placingOrder.length) {
                                int nextColor = placingOrder[placingCount];
                                // 해당 플레이어가 몇 번째 돌을 두는지 계산
                                int num = 1;
                                for (int i = 0; i < placingCount; i++) if (placingOrder[i] == nextColor) num++;
                                // 전체 몇 개 두는지
                                int total = 0;
                                for (int v : placingOrder) if (v == nextColor) total++;
                                statusLabel.setText(playerNames[nextColor] + ": 빈 칸에 말을 놓으세요 (" + (num) + "/" + total + ")");
                            } else {
                                placingPhase = false;
                                currentPlayer = 1;
                                statusLabel.setText(playerNames[currentPlayer] + " 차례: 말을 선택하세요");
                            }
                            repaint();
                        }
                        return;
                    }
                    if (waitingForWall) {
                        if (lastMovedStone == null) return;
                        int sx = lastMovedStone.x;
                        int sy = lastMovedStone.y;
                        int dx = x - sx;
                        int dy = y - sy;
                        int color = getCurrentPlayer();
                        if (Math.abs(dx) + Math.abs(dy) != 1) return;
                        if (dx == 1 && sx < BOARD_SIZE-1 && vWalls[sx+1][sy] == 0) {
                            vWalls[sx+1][sy] = color;
                        } else if (dx == -1 && sx > 0 && vWalls[sx][sy] == 0) {
                            vWalls[sx][sy] = color;
                        } else if (dy == 1 && sy < BOARD_SIZE-1 && hWalls[sy+1][sx] == 0) {
                            hWalls[sy+1][sx] = color;
                        } else if (dy == -1 && sy > 0 && hWalls[sy][sx] == 0) {
                            hWalls[sy][sx] = color;
                        } else {
                            return;
                        }
                        waitingForWall = false;
                        lastMovedStone = null;
                        movableSpots.clear();
                        nextTurn();
                        statusLabel.setText(playerNames[getCurrentPlayer()] + " 차례: 말을 선택하세요");
                        repaint();
                        checkGameEnd();
                        return;
                    }
                    // 말 선택/이동
                    if (selectedStone == null) {
                        if (board[y][x] == getCurrentPlayer()) {
                            selectedStone = new Point(x, y);
                            statusLabel.setText(playerNames[getCurrentPlayer()] + " 이동할 위치를 선택하세요" + (wallBreakMode ? " (벽 부수기 가능)" : ""));
                            movableSpots = getMovableSpots(x, y, wallBreakMode);
                            repaint();
                        }
                    } else {
                        if (canMove(selectedStone.x, selectedStone.y, x, y, wallBreakMode)) {
                            saveState();
                            if (wallBreakMode) {
                                breakWallOnPath(selectedStone.x, selectedStone.y, x, y);
                                wallBreakMode = false;
                            }
                            if (!(selectedStone.x == x && selectedStone.y == y)) {
                                board[y][x] = board[selectedStone.y][selectedStone.x];
                                board[selectedStone.y][selectedStone.x] = 0;
                            }
                            lastMovedStone = new Point(x, y);
                            selectedStone = null;
                            waitingForWall = true;
                            movableSpots.clear();
                            statusLabel.setText(playerNames[getCurrentPlayer()] + " 이동한 말의 상하좌우에 벽을 설치하세요");
                            repaint();
                        } else {
                            selectedStone = null;
                            movableSpots.clear();
                            wallBreakMode = false;
                            statusLabel.setText(playerNames[getCurrentPlayer()] + " 차례: 말을 선택하세요");
                            repaint();
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // 바둑판
            g.setColor(new Color(222, 184, 135));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
            // 영역 표시 (게임 종료 시)
            if (gameEnded) {
                int[][] areaMap = getAreaMap();
                for (int y = 0; y < BOARD_SIZE; y++) {
                    for (int x = 0; x < BOARD_SIZE; x++) {
                        if (areaMap[y][x] == 1) {
                            g.setColor(new Color(200, 200, 200, 100)); // 흑: 연회색
                            g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        } else if (areaMap[y][x] == 2) {
                            g.setColor(new Color(100, 180, 255, 100)); // 백: 연파랑
                            g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        }
                    }
                }
            }
            // 이동 가능 위치 표시
            g.setColor(new Color(0, 200, 0, 100));
            for (Point p : movableSpots) {
                g.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
            // 벽 그리기
            for (int y = 0; y <= BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    if (y < hWalls.length && x < hWalls[0].length && hWalls[y][x] != 0) {
                        g.setColor(wallColors[hWalls[y][x]]);
                        g.fillRect(x * CELL_SIZE, y * CELL_SIZE - 3, CELL_SIZE, 6);
                    }
                }
            }
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x <= BOARD_SIZE; x++) {
                    if (x < vWalls.length && y < vWalls[0].length && vWalls[x][y] != 0) {
                        g.setColor(wallColors[vWalls[x][y]]);
                        g.fillRect(x * CELL_SIZE - 3, y * CELL_SIZE, 6, CELL_SIZE);
                    }
                }
            }
            // 돌 그리기
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    if (board[y][x] != 0) {
                        g.setColor(playerColors[board[y][x]]);
                        g.fillOval(x * CELL_SIZE + (CELL_SIZE - STONE_SIZE) / 2, y * CELL_SIZE + (CELL_SIZE - STONE_SIZE) / 2, STONE_SIZE, STONE_SIZE);
                        g.setColor(Color.BLACK);
                        g.drawOval(x * CELL_SIZE + (CELL_SIZE - STONE_SIZE) / 2, y * CELL_SIZE + (CELL_SIZE - STONE_SIZE) / 2, STONE_SIZE, STONE_SIZE);
                    }
                }
            }
            // 선택 표시
            if (selectedStone != null) {
                g.setColor(Color.RED);
                g.drawRect(selectedStone.x * CELL_SIZE + 2, selectedStone.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
            }
            // 벽 설치 가능 위치 표시
            if (waitingForWall && lastMovedStone != null) {
                int sx = lastMovedStone.x;
                int sy = lastMovedStone.y;
                g.setColor(new Color(0, 128, 255, 128));
                if (sx < BOARD_SIZE-1 && vWalls[sx+1][sy] == 0) g.fillRect((sx+1)*CELL_SIZE-8, sy*CELL_SIZE+8, 16, CELL_SIZE-16);
                if (sx > 0 && vWalls[sx][sy] == 0) g.fillRect(sx*CELL_SIZE-8, sy*CELL_SIZE+8, 16, CELL_SIZE-16);
                if (sy < BOARD_SIZE-1 && hWalls[sy+1][sx] == 0) g.fillRect(sx*CELL_SIZE+8, (sy+1)*CELL_SIZE-8, CELL_SIZE-16, 16);
                if (sy > 0 && hWalls[sy][sx] == 0) g.fillRect(sx*CELL_SIZE+8, sy*CELL_SIZE-8, CELL_SIZE-16, 16);
            }
        }
    }

    // 이동 가능한 위치 리스트 반환 (벽 부수기 모드 지원)
    private java.util.List<Point> getMovableSpots(int sx, int sy, boolean canBreakWall) {
        java.util.List<Point> spots = new ArrayList<>();
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int d1 = 0; d1 < 4; d1++) {
            int mx = sx + dx[d1];
            int my = sy + dy[d1];
            if (mx < 0 || mx >= BOARD_SIZE || my < 0 || my >= BOARD_SIZE) continue;
            if (board[my][mx] != 0) continue;
            boolean wall1 = !canStep(sx, sy, mx, my);
            if (wall1 && !canBreakWall) continue;
            // 중간에 멈추는 것도 가능
            spots.add(new Point(mx, my));
            for (int d2 = 0; d2 < 4; d2++) {
                int ex = mx + dx[d2];
                int ey = my + dy[d2];
                if (ex < 0 || ex >= BOARD_SIZE || ey < 0 || ey >= BOARD_SIZE) continue;
                if (board[ey][ex] != 0) continue;
                boolean wall2 = !canStep(mx, my, ex, ey);
                int wallCount = (wall1 ? 1 : 0) + (wall2 ? 1 : 0);
                if (wallCount > (canBreakWall ? 1 : 0)) continue;
                spots.add(new Point(ex, ey));
            }
        }
        // 제자리 이동 허용
        spots.add(new Point(sx, sy));
        return spots;
    }

    // canMove도 벽 부수기 모드 지원
    private boolean canMove(int sx, int sy, int ex, int ey, boolean canBreakWall) {
        if (sx == ex && sy == ey) return true;
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int d1 = 0; d1 < 4; d1++) {
            int mx = sx + dx[d1];
            int my = sy + dy[d1];
            if (mx < 0 || mx >= BOARD_SIZE || my < 0 || my >= BOARD_SIZE) continue;
            if (board[my][mx] != 0) continue;
            boolean wall1 = !canStep(sx, sy, mx, my);
            if (wall1 && !canBreakWall) continue;
            if (mx == ex && my == ey) return true;
            for (int d2 = 0; d2 < 4; d2++) {
                int tx = mx + dx[d2];
                int ty = my + dy[d2];
                if (tx < 0 || tx >= BOARD_SIZE || ty < 0 || ty >= BOARD_SIZE) continue;
                if (board[ty][tx] != 0) continue;
                boolean wall2 = !canStep(mx, my, tx, ty);
                int wallCount = (wall1 ? 1 : 0) + (wall2 ? 1 : 0);
                if (wallCount > (canBreakWall ? 1 : 0)) continue;
                if (tx == ex && ty == ey) return true;
            }
        }
        return false;
    }

    // 게임 종료 및 영역 판정: 각 플레이어의 말이 연결되어 있지 않으면 종료
    private void checkGameEnd() {
        // 각 플레이어별 BFS로 연결된 말 그룹 찾기
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        int[] groupCount = new int[3];
        for (int color = 1; color <= 2; color++) {
            outer: for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    if (!visited[y][x] && board[y][x] == color) {
                        int cnt = bfsCount(x, y, color, visited);
                        groupCount[color]++;
                        break outer;
                    }
                }
            }
        }
        // 둘 중 한 명이라도 말이 분리되어 있으면 종료
        if (groupCount[1] > 1 || groupCount[2] > 1) {
            gameEnded = true;
            int area1 = countArea(1);
            int area2 = countArea(2);
            String msg = "게임 종료! ";
            if (area1 > area2) msg += playerNames[1] + " 승리!";
            else if (area2 > area1) msg += playerNames[2] + " 승리!";
            else msg += "무승부!";
            statusLabel.setText(msg + " (흑: " + area1 + ", 백: " + area2 + ")");
        }
    }

    // BFS로 연결된 말 개수 세기
    private int bfsCount(int sx, int sy, int color, boolean[][] visited) {
        int cnt = 0;
        Queue<Point> q = new LinkedList<>();
        q.add(new Point(sx, sy));
        visited[sy][sx] = true;
        while (!q.isEmpty()) {
            Point p = q.poll();
            cnt++;
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            for (int d = 0; d < 4; d++) {
                int nx = p.x + dx[d];
                int ny = p.y + dy[d];
                if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
                if (visited[ny][nx]) continue;
                // 벽 체크
                if (d == 0 && vWalls[p.x+1][p.y] != 0) continue;
                if (d == 1 && vWalls[p.x][p.y] != 0) continue;
                if (d == 2 && hWalls[p.y+1][p.x] != 0) continue;
                if (d == 3 && hWalls[p.y][p.x] != 0) continue;
                if (board[ny][nx] == color) {
                    visited[ny][nx] = true;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return cnt;
    }

    // 각 플레이어의 영역(연결된 말 포함 구역) 크기 세기
    private int countArea(int color) {
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        int area = 0;
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (!visited[y][x] && board[y][x] == color) {
                    area += bfsArea(x, y, color, visited);
                }
            }
        }
        return area;
    }
    private int bfsArea(int sx, int sy, int color, boolean[][] visited) {
        int area = 0;
        Queue<Point> q = new LinkedList<>();
        q.add(new Point(sx, sy));
        visited[sy][sx] = true;
        while (!q.isEmpty()) {
            Point p = q.poll();
            area++;
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            for (int d = 0; d < 4; d++) {
                int nx = p.x + dx[d];
                int ny = p.y + dy[d];
                if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
                if (visited[ny][nx]) continue;
                // 벽 체크
                if (d == 0 && vWalls[p.x+1][p.y] != 0) continue;
                if (d == 1 && vWalls[p.x][p.y] != 0) continue;
                if (d == 2 && hWalls[p.y+1][p.x] != 0) continue;
                if (d == 3 && hWalls[p.y][p.x] != 0) continue;
                if (board[ny][nx] == color) {
                    visited[ny][nx] = true;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return area;
    }

    // 게임 종료 시 각 칸의 소유자(1:흑, 2:백, 0:공용/빈칸) 맵 반환
    private int[][] getAreaMap() {
        int[][] areaMap = new int[BOARD_SIZE][BOARD_SIZE];
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (!visited[y][x] && board[y][x] != 0) {
                    int color = board[y][x];
                    java.util.List<Point> group = new ArrayList<>();
                    Queue<Point> q = new LinkedList<>();
                    q.add(new Point(x, y));
                    visited[y][x] = true;
                    boolean onlyOneColor = true;
                    while (!q.isEmpty()) {
                        Point p = q.poll();
                        group.add(p);
                        int[] dx = {1, -1, 0, 0};
                        int[] dy = {0, 0, 1, -1};
                        for (int d = 0; d < 4; d++) {
                            int nx = p.x + dx[d];
                            int ny = p.y + dy[d];
                            if (nx < 0 || nx >= BOARD_SIZE || ny < 0 || ny >= BOARD_SIZE) continue;
                            if (visited[ny][nx]) continue;
                            // 벽 체크
                            if (d == 0 && vWalls[p.x+1][p.y] != 0) continue;
                            if (d == 1 && vWalls[p.x][p.y] != 0) continue;
                            if (d == 2 && hWalls[p.y+1][p.x] != 0) continue;
                            if (d == 3 && hWalls[p.y][p.x] != 0) continue;
                            if (board[ny][nx] != 0) {
                                if (board[ny][nx] != color) onlyOneColor = false;
                                visited[ny][nx] = true;
                                q.add(new Point(nx, ny));
                            }
                        }
                    }
                    for (Point p : group) {
                        areaMap[p.y][p.x] = onlyOneColor ? color : 0;
                    }
                }
            }
        }
        return areaMap;
    }

    // 상태 저장
    private void saveState() {
        GameState s = new GameState();
        s.board = new int[BOARD_SIZE][BOARD_SIZE];
        s.hWalls = new int[BOARD_SIZE+1][BOARD_SIZE];
        s.vWalls = new int[BOARD_SIZE][BOARD_SIZE+1];
        for (int i = 0; i < BOARD_SIZE; i++)
            System.arraycopy(board[i], 0, s.board[i], 0, BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE+1; i++)
            System.arraycopy(hWalls[i], 0, s.hWalls[i], 0, BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++)
            System.arraycopy(vWalls[i], 0, s.vWalls[i], 0, BOARD_SIZE+1);
        s.blackTurn = blackTurn;
        s.waitingForWall = waitingForWall;
        s.lastMovedStone = lastMovedStone == null ? null : new Point(lastMovedStone);
        s.placingPhase = placingPhase;
        s.placingCount = placingCount;
        s.stonesPlaced = stonesPlaced.clone();
        history.push(s);
    }
    // 상태 복원
    private void undo() {
        if (history.isEmpty()) return;
        GameState s = history.pop();
        for (int i = 0; i < BOARD_SIZE; i++)
            System.arraycopy(s.board[i], 0, board[i], 0, BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE+1; i++)
            System.arraycopy(s.hWalls[i], 0, hWalls[i], 0, BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++)
            System.arraycopy(s.vWalls[i], 0, vWalls[i], 0, BOARD_SIZE+1);
        blackTurn = s.blackTurn;
        waitingForWall = s.waitingForWall;
        lastMovedStone = s.lastMovedStone == null ? null : new Point(s.lastMovedStone);
        placingPhase = s.placingPhase;
        placingCount = s.placingCount;
        stonesPlaced = s.stonesPlaced.clone();
        wallBreakMode = false;
        movableSpots.clear();
        statusLabel.setText(playerNames[1] + ": 빈 칸에 말을 놓으세요 (1/" + (placingOrder.length/2) + ")");
        repaint();
    }

    // 이동 경로에서 벽 1개 부수기
    private void breakWallOnPath(int sx, int sy, int ex, int ey) {
        // 가장 먼저 만나는 벽 1개만 부수고 이동
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int d1 = 0; d1 < 4; d1++) {
            int mx = sx + dx[d1];
            int my = sy + dy[d1];
            if (mx < 0 || mx >= BOARD_SIZE || my < 0 || my >= BOARD_SIZE) continue;
            if (board[my][mx] != 0) continue;
            boolean wall1 = !canStep(sx, sy, mx, my);
            if (mx == ex && my == ey) {
                if (wall1) removeWallBetween(sx, sy, mx, my);
                return;
            }
            for (int d2 = 0; d2 < 4; d2++) {
                int tx = mx + dx[d2];
                int ty = my + dy[d2];
                if (tx < 0 || tx >= BOARD_SIZE || ty < 0 || ty >= BOARD_SIZE) continue;
                if (board[ty][tx] != 0) continue;
                boolean wall2 = !canStep(mx, my, tx, ty);
                int wallCount = (wall1 ? 1 : 0) + (wall2 ? 1 : 0);
                if (wallCount > 1) continue;
                if (tx == ex && ty == ey) {
                    if (wall1) removeWallBetween(sx, sy, mx, my);
                    else if (wall2) removeWallBetween(mx, my, tx, ty);
                    return;
                }
            }
        }
    }
    // 두 칸 사이의 벽 제거
    private void removeWallBetween(int x1, int y1, int x2, int y2) {
        if (x1 == x2) {
            if (y1 < y2) hWalls[y2][x1] = 0;
            else hWalls[y1][x1] = 0;
        } else if (y1 == y2) {
            if (x1 < x2) vWalls[x2][y1] = 0;
            else vWalls[x1][y1] = 0;
        }
    }

    // 한 칸 이동, 벽 통과 불가
    private boolean canStep(int sx, int sy, int ex, int ey) {
        if (Math.abs(sx - ex) + Math.abs(sy - ey) != 1) return false;
        if (ex < 0 || ex >= BOARD_SIZE || ey < 0 || ey >= BOARD_SIZE) return false;
        if (board[ey][ex] != 0) return false;
        if (sx < ex && vWalls[sx+1][sy] != 0) return false;
        if (sx > ex && vWalls[sx][sy] != 0) return false;
        if (sy < ey && hWalls[sy+1][sx] != 0) return false;
        if (sy > ey && hWalls[sy][sx] != 0) return false;
        return true;
    }

    // 현재 차례 플레이어 번호 반환
    private int getCurrentPlayer() {
        return currentPlayer;
    }
    // 다음 차례로 넘기기
    private void nextTurn() {
        currentPlayer++;
        if (currentPlayer > playerCount) currentPlayer = 1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WallBadukZoneGame game = new WallBadukZoneGame();
            game.setVisible(true);
        });
    }
} 