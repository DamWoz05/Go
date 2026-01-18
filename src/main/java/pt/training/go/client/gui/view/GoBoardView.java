package pt.training.go.client.gui.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import pt.training.go.client.gui.model.BoardPoint;
import pt.training.go.server.StoneColor;

import java.util.function.Consumer;

/**
 * Widok planszy Go rysowany na Canvas.
 *
 * Funkcje:
 * - rysuje siatke planszy i numeracje 1..N dookola,
 * - rysuje kamienie na podstawie danych z serwera,
 * - wykrywa pole wskazywane myszka i klikniete,
 *
 * Reprezentacja planszy:
 * - znak '+' oznacza puste pole,
 * - znaki kamieni pochodza z StoneColor.asChar(),
 * - znak 'x' oznacza kamien oznaczony jako martwy w fazie scoring.
 */
public final class GoBoardView extends StackPane {

    private final Canvas canvas = new Canvas();

    private int size = 0;

    private char[][] cells;
    private char[][] lastKnownStones;

    private BoardPoint hover;
    private BoardPoint selected;

    private Consumer<BoardPoint> onHover;
    private Consumer<BoardPoint> onClick;

    private boolean scoringMode = false;

    /**
     * Tworzy widok planszy i podpina obsluge myszy.
     * Canvas automatycznie dopasowuje rozmiar do rozmiaru StackPane.
     */
    public GoBoardView() {
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((o,a,b) -> redraw());
        heightProperty().addListener((o,a,b) -> redraw());

        setOnMouseMoved(e -> {
            BoardPoint p = pickIntersection(e.getX(), e.getY());
            if ((p == null && hover != null) || (p != null && !p.equals(hover))) {
                hover = p;
                if (onHover != null) onHover.accept(hover);
                redraw();
            }
        });
        setOnMouseExited(e -> {
            if (hover != null) {
                hover = null;
                if (onHover != null) onHover.accept(null);
                redraw();
            }
        });

        setOnMouseClicked(e -> {
            BoardPoint p = pickIntersection(e.getX(), e.getY());
            if (p != null) {
                selected = p;
                if (onClick != null) onClick.accept(p);
                redraw();
            }
        });
    }

    /**
     * Ustawia callback wywolywany przy zmianie hover.
     *
     * @param onHover callback
     */
    public void setOnHover(Consumer<BoardPoint> onHover) {
        this.onHover = onHover;
    }


    /**
     * Ustawia callback wywolywany po kliknieciu pola planszy.
     *
     * @param onClick callback
     */
    public void setOnClick(Consumer<BoardPoint> onClick) {
        this.onClick = onClick;
    }

    /**
     * Ustawia rozmiar planszy NxN i inicjalizuje bufory.
     *
     * @param n rozmiar planszy (5-19)
     */
    public void setBoardSize(int n) {
        this.size = n;
        if (n <= 0) {
            this.cells = null;
            this.lastKnownStones = null;
            redraw();
            return;
        }

        this.cells = new char[n][n];
        this.lastKnownStones = new char[n][n];

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                cells[r][c] = '+';
                lastKnownStones[r][c] = '+';
            }
        }

        hover = null;
        selected = null;
        redraw();
    }

    /**
     * Aktualizuje zawartosc planszy na podstawie flat stringa.
     *
     * @param flat reprezentacja planszy o dlugosci size*size, czytana wierszami
     */
    public void setBoardFlat(String flat) {
        if (size <= 0 || flat == null) return;
        if (flat.length() != size * size) return;

        int idx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                char ch = flat.charAt(idx++);
                cells[r][c] = ch;

                if (ch != '+' && ch != 'x') {
                    lastKnownStones[r][c] = ch;
                }
            }
        }
        redraw();
    }

    /**
     * Przerysowuje cala plansze na Canvas.
     *
     * Kolejnosc:
     * 1) wyczyszczenie tla,
     * 2) policzenie geometrii (pozycja planszy, marginesy, odstepy),
     * 3) tlo "drewniane",
     * 4) siatka linii,
     * 5) numeracja 1..N dookola,
     * 6) kamienie (oraz czerwone X dla 'x' w scoring),
     * 7) znacznik obserwowane (zolty),
     * 8) znacznik zaznaczone (zielony).
     */
    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        g.clearRect(0, 0, w, h);

        if (size <= 0) return;

        BoardGeom geom = computeGeom(w, h);
        double x0 = geom.x0;
        double y0 = geom.y0;
        double step = geom.step;
        double boardSide = geom.boardSide;
        double margin = geom.margin;

        g.setFill(Color.rgb(230, 210, 160));
        g.fillRect(x0 - margin, y0 - margin, boardSide + 2 * margin, boardSide + 2 * margin);

        g.setStroke(Color.BLACK);
        g.setLineWidth(1.2);

        for (int i = 0; i < size; i++) {
            double x = x0 + i * step;
            double y = y0 + i * step;

            g.strokeLine(x, y0, x, y0 + (size - 1) * step);
            g.strokeLine(x0, y, x0 + (size - 1) * step, y);
        }

        g.setFill(Color.BLACK);
        for (int i = 0; i < size; i++) {
            String t = String.valueOf(i + 1);
            double x = x0 + i * step;

            g.fillText(t, x - 5, y0 - 10);                           // gora
            g.fillText(t, x - 5, y0 + (size - 1) * step + 22);       // dol
        }
        for (int i = 0; i < size; i++) {
            String t = String.valueOf(i + 1);
            double y = y0 + i * step;

            g.fillText(t, x0 - 22, y + 4);                           // lewo
            g.fillText(t, x0 + (size - 1) * step + 12, y + 4);        // prawo
        }

        double rStone = step * 0.42;
        char blackChar = StoneColor.CZARNY.asChar();
        char whiteChar = StoneColor.BIALY.asChar();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                char ch = cells[r][c];
                if (ch == '+') continue;

                boolean isDead = (ch == 'x');
                char baseStone = isDead ? lastKnownStones[r][c] : ch;
                boolean isBlack = baseStone == blackChar;
                boolean isWhite = baseStone == whiteChar;

                if (!isBlack && !isWhite) continue;

                double cx = x0 + c * step;
                double cy = y0 + r * step;

                if (isBlack) {
                    g.setFill(Color.BLACK);
                    g.fillOval(cx - rStone, cy - rStone, 2 * rStone, 2 * rStone);
                } else {
                    g.setFill(Color.WHITE);
                    g.fillOval(cx - rStone, cy - rStone, 2 * rStone, 2 * rStone);
                    g.setStroke(Color.BLACK);
                    g.strokeOval(cx - rStone, cy - rStone, 2 * rStone, 2 * rStone);
                }

                if (isDead) {
                    g.setStroke(Color.RED);
                    g.setLineWidth(2.0);
                    g.strokeLine(cx - rStone * 0.6, cy - rStone * 0.6, cx + rStone * 0.6, cy + rStone * 0.6);
                    g.strokeLine(cx - rStone * 0.6, cy + rStone * 0.6, cx + rStone * 0.6, cy - rStone * 0.6);
                    g.setLineWidth(1.2);
                }
            }
        }

        if (hover != null) {
            double cx = x0 + hover.col0() * step;
            double cy = y0 + hover.row0() * step;

            g.setStroke(Color.YELLOW);
            g.setLineWidth(2.0);
            g.strokeOval(cx - rStone * 0.25, cy - rStone * 0.25, rStone * 0.5, rStone * 0.5);
            g.setLineWidth(1.2);
        }

        if (selected != null) {
            double cx = x0 + selected.col0() * step;
            double cy = y0 + selected.row0() * step;

            g.setStroke(Color.LIMEGREEN);
            g.setLineWidth(2.5);
            g.strokeOval(cx - rStone * 0.35, cy - rStone * 0.35, rStone * 0.7, rStone * 0.7);
            g.setLineWidth(1.2);
        }
    }

    /**
     * Zamienia wspolrzedne myszy na najblizsze skrzyzowanie linii planszy.
     *
     * Dzialanie:
     * - przelicza pozycje myszy na wspolrzedne "w ulamkach kroku" (fx/fy),
     * - zaokragla do najblizszego indeksu (r,c),
     * - odrzuca klik/hover jesli jest poza plansza,
     * - dodatkowo stosuje tolerancje, zeby nie lapac punktow zbyt daleko od skrzyzowania.
     *
     * Zwraca null, gdy nie trafiono w zadne sensowne skrzyzowanie.
     */
    private BoardPoint pickIntersection(double mx, double my) {
        if (size <= 0) return null;

        BoardGeom geom = computeGeom(canvas.getWidth(), canvas.getHeight());
        if (size == 1) return null;

        double x0 = geom.x0;
        double y0 = geom.y0;
        double step = geom.step;

        double fx = (mx - x0) / step;
        double fy = (my - y0) / step;

        int c = (int) Math.round(fx);
        int r = (int) Math.round(fy);

        if (r < 0 || r >= size || c < 0 || c >= size) return null;

        double cx = x0 + c * step;
        double cy = y0 + r * step;

        double tol = step * 0.5;
        if (Math.abs(mx - cx) > tol || Math.abs(my - cy) > tol) return null;

        return new BoardPoint(r, c);
    }

    /**
     * Liczy geometrie rysowania planszy w zaleznosci od rozmiaru Canvas.
     *
     * Wynik:
     * - margin: odstep od krawedzi (musi zmiescic numeracje),
     * - boardSide: dlugosc boku kwadratu planszy,
     * - (x0,y0): lewy gorny punkt planszy,
     * - step: odstep miedzy liniami (zalezy od size).
     *
     * Plansza jest centrowana w Canvas.
     */
    private BoardGeom computeGeom(double w, double h) {
        double margin = Math.max(28, Math.min(w, h) * 0.08);
        double boardSide = Math.min(w, h) - 2 * margin;
        boardSide = Math.max(10, boardSide);

        double x0 = (w - boardSide) / 2.0;
        double y0 = (h - boardSide) / 2.0;

        double step = (size <= 1) ? boardSide : boardSide / (size - 1);

        return new BoardGeom(x0, y0, boardSide, margin, step);
    }

    private record BoardGeom(double x0, double y0, double boardSide, double margin, double step) {}


    /**
     * Ustawia pole zaznaczone
     *
     * @param p punkt planszy lub null
     */
    public void setSelected(BoardPoint p) {
        this.selected = p;
        redraw();
    }

    /**
     * Sprawdza czy w danym punkcie jest oznaczenie martwego kamienia ('x').
     * Uzywane w fazie SCORING do decyzji czy prosic o potwierdzenie.
     *
     * @param p punkt planszy (0-based)
     * @return true jesli w komorce jest 'x'
     */
    public boolean isDeadAt(BoardPoint p) {
        if (p == null || size <= 0 || cells == null) return false;
        int r = p.row0();
        int c = p.col0();
        if (r < 0 || r >= size || c < 0 || c >= size) return false;
        return cells[r][c] == 'x';
    }
}
