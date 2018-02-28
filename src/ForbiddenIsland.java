// Assignment 9 Part 1
// Bauer Zach
// zbauer
// Nahar Ateev
// nahar

// importing libraries
import java.util.ArrayList;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// class to maintain game constants
class SceneSettings {
  // the width and height of each cell
  final int CELL_SIZE = 10;
  // the number of tiles across the island
  final int ISLAND_SIZE = 64;
  // the height of the island, in pixels
  final int HEIGHT = CELL_SIZE * (ISLAND_SIZE + 1);
  // the width of the island, in pixels
  final int WIDTH = HEIGHT;
  // the max height that the island can be
  final int MAX_HEIGHT = ISLAND_SIZE / 2;
  // the number of targets on the island (including the helicopter)
  final int numTargets = 2;
}

// Represents a single square of the game area
class Cell {
  // represents absolute height of this cell, in feet
  double height;
  // In logical coordinates, with the origin at the top-left corner of the
  // screen
  int x;
  int y;
  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;
  // reports whether this cell is flooded or not
  boolean isFlooded;

  // Constructor with all fields as parameters
  Cell(double height, int x, int y, Cell left, Cell top, Cell right, Cell bottom,
      boolean isFlooded) {
    this.height = height;
    this.x = x;
    this.y = y;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.isFlooded = isFlooded;
  }

  // Constructor without accounting for neighbor cells and isFlooded (for
  // initializing)
  Cell(double height, int x, int y, boolean isFlooded) {
    this.height = height;
    this.x = x;
    this.y = y;
    this.isFlooded = isFlooded;
  }

  // object for accessing settings class
  SceneSettings ss = new SceneSettings();

  // returns the image for this Cell to be drawn
  public WorldImage getImage(int waterHeight) {
    WorldImage img = new RectangleImage(ss.CELL_SIZE, ss.CELL_SIZE, OutlineMode.SOLID,
        this.getColor(waterHeight));
    return img;
  }

  // gets the color for this Cell based on the height
  Color getColor(int waterHeight) {
    int h = (int) Math.round(this.height);
    // if the cell is in the ocean
    if (h == 0) {
      return Color.BLUE;
    }
    // sliding scales to represent rgb values
    int r = (h - waterHeight) * 8;
    int b = (h - waterHeight) * 8;
    int g = 95 + (h - waterHeight) * 5;

    // verifying that the rgb values are within boundaries
    if (b > 255) {
      b = 255;
    }
    else if (b < 0) {
      b = 0;
    }
    if (r > 255) {
      r = 255;
    }
    else if (r < 0) {
      r = 0;
    }
    if (g > 255) {
      g = 255;
    }
    else if (g < 0) {
      g = 0;
    }

    // if the cell is flooded but not ocean
    if (isFlooded) {
      if (h - waterHeight == 0) {
        return new Color(102, 255, 255);
      }
      else if (h - waterHeight == -1) {
        return new Color(51, 153, 255);
      }
      else if (h - waterHeight == -2) {
        return new Color(0, 0, 255);
      }
      else {
        return Color.BLACK;
      }
    }
    // if the cell is not flooded
    else {
      // if the cell is below water level
      if (h <= waterHeight) {
        r = (waterHeight - h) * 5;
        if (r > 255) {
          r = 255;
        }
        else if (r < 0) {
          r = 0;
        }
        return new Color(r, g, b);
      }
      return new Color(r, g, b);
    }
  }
}

// representation of a player
class Player {
  int x;
  int y;
  WorldImage img;

  // constructor for the player class
  Player(int x, int y) {
    this.x = x;
    this.y = y;
    this.img = new FromFileImage("pilot-icon.png");
  }

  // draws this Player in the WorldScene
  void draw(WorldScene w, int waterHeight) {
    SceneSettings ss = new SceneSettings();
    w.placeImageXY(this.img, this.x * ss.CELL_SIZE + ss.CELL_SIZE / 2,
        this.y * ss.CELL_SIZE + ss.CELL_SIZE / 2);
  }

  // used to determine which way to move a player based on key events
  void move(String key, Cell current) {
    if (key.equals("right") || key.equals("d")) {
      if (!current.right.isFlooded) {
        this.x += 1;
      }
    }
    else if (key.equals("left") || key.equals("a")) {
      if (!current.left.isFlooded) {
        this.x -= 1;
      }
    }
    else if (key.equals("down") || key.equals("s")) {
      if (!current.bottom.isFlooded) {
        this.y += 1;
      }
    }
    else if (key.equals("up") || key.equals("w")) {
      if (!current.top.isFlooded) {
        this.y -= 1;
      }
    }
  }
}

// representation of helicopter parts or "targets"
class Target {
  int x;
  int y;
  WorldImage img;
  boolean isCollected;

  // constructor for Target class
  Target(int x, int y) {
    this.x = x;
    this.y = y;
    this.img = new CircleImage(5, OutlineMode.SOLID, Color.MAGENTA);
  }

  // draws this Target in the WorldScene
  void draw(WorldScene w, int waterHeight) {
    SceneSettings ss = new SceneSettings();
    if (!isCollected) {
      w.placeImageXY(this.img, this.x * ss.CELL_SIZE + ss.CELL_SIZE / 2,
          this.y * ss.CELL_SIZE + ss.CELL_SIZE / 2);
    }
  }

  // checks to see if the given player is at the same position as this target
  public boolean samePosn(Player p) {
    return p.x == this.x && p.y == this.y;
  }
}

// represents helicopter
class HelicopterTarget extends Target {
  boolean canBeCollected = false;

  // constructor for helicopter
  HelicopterTarget(int x, int y) {
    super(x, y);
    this.img = new FromFileImage("helicopter.png");
  }
}

// class for game mechanics
class ForbiddenIslandWorld extends World {
  // All of the heights used to generate cells
  ArrayList<ArrayList<Double>> heights;
  // All the cells of the game, including the ocean
  public ArrayList<ArrayList<Cell>> cells;
  // All of the cells in the game in one IList, with updated links
  IList<Cell> board;
  // the current water height in the game
  int waterHeight;
  // the player
  Player player;
  // the second player
  Player player2;
  // the IList of all targets
  IList<Target> targets;
  // the helicopter
  HelicopterTarget heli;
  // an accumulator for determining how many ticks have passed
  int tickNum = 1;
  // an accumulator for keeping track of how many targets have been collected
  ArrayList<Target> collected = new ArrayList<Target>();
  // for distinguishing between menu screen and game
  boolean isStarted = false;

  // spawns the player randomly
  public Player playerSpawn() {
    double x = 0;
    double y = 0;
    while (this.cells.get((int) x).get((int) y).isFlooded) {
      x = Math.random() * ss.ISLAND_SIZE;
      y = Math.random() * ss.ISLAND_SIZE;
    }
    Player p = new Player((int) x, (int) y);
    return p;
  }

  // spawns the helicopter at the first peak that reaches max height
  // ... if no cells at max height, spawns at center
  public HelicopterTarget makeHeli() {
    HelicopterTarget t;
    int x = 32;
    int y = 32;
    for (int i = 0; i < this.cells.size(); i++) {
      for (int j = 0; j < this.cells.size(); j++) {
        Cell temp = this.cells.get(i).get(j);
        if (temp.height == ss.MAX_HEIGHT) {
          x = i;
          y = j;
        }
      }
    }
    t = new HelicopterTarget(x, y);
    return t;
  }

  // spawns the list of targets
  public IList<Target> makeTargets() {
    IList<Target> targets = new MtList<Target>();
    Target t;
    for (int i = 1; i < ss.numTargets; i++) {
      double x = 0;
      double y = 0;
      while (this.cells.get((int) x).get((int) y).isFlooded) {
        x = Math.random() * ss.ISLAND_SIZE;
        y = Math.random() * ss.ISLAND_SIZE;
      }
      t = new Target((int) x, (int) y);
      targets = targets.append(t);
    }
    return targets;
  }

  // returns the ArrayList<ArrayList<Cell>> as an IList<Cell>
  // interp. converts the nested ArrayList into a linked list
  IList<Cell> update() {
    IList<Cell> board = new MtList<Cell>();
    for (int i = 0; i < this.cells.size(); i++) {
      for (int j = 0; j < this.cells.size(); j++) {
        boolean isFlooded = false;
        Cell temp = this.cells.get(i).get(j);
        if (temp.height <= this.waterHeight) {
          if (temp.left.isFlooded || temp.right.isFlooded || temp.top.isFlooded
              || temp.bottom.isFlooded) {
            isFlooded = true;
            temp.isFlooded = true;
          }
        }
        Cell c = new Cell(temp.height, temp.x, temp.y, temp.left, temp.top, temp.right, temp.bottom,
            isFlooded);
        board = board.append(c);
      }
    }
    return board;
  }

  // returns the ArrayList<ArrayList<Cell>> as an IList<Cell>
  // interp. converts the nested ArrayList into a linked list
  // ... also updates links between cells
  IList<Cell> toIList() {
    Cell top = null;
    Cell left = null;
    Cell right = null;
    Cell bottom = null;
    IList<Cell> board = new MtList<Cell>();
    for (int i = 0; i < this.cells.size(); i++) {
      for (int j = 0; j < this.cells.size(); j++) {
        boolean isFlooded = false;
        Cell temp = this.cells.get(i).get(j);
        if (j == 0) {
          top = this.cells.get(i).get(j);
          temp.top = top;
          bottom = this.cells.get(i).get(j + 1);
          temp.bottom = bottom;
        }
        else if (j == this.cells.size() - 1) {
          bottom = this.cells.get(i).get(j);
          temp.bottom = bottom;
          top = this.cells.get(i).get(j - 1);
          temp.top = top;
        }
        else {
          top = this.cells.get(i).get(j - 1);
          temp.top = top;
          bottom = this.cells.get(i).get(j + 1);
          temp.bottom = bottom;
        }
        if (i == 0) {
          left = this.cells.get(i).get(j);
          temp.left = left;
          right = this.cells.get(i + 1).get(j);
          temp.right = right;
        }
        else if (i == this.cells.size() - 1) {
          right = this.cells.get(i).get(j);
          temp.right = right;
          left = this.cells.get(i - 1).get(j);
          temp.left = left;
        }
        else {
          left = this.cells.get(i - 1).get(j);
          temp.left = left;
          right = this.cells.get(i + 1).get(j);
          temp.right = right;
        }
        if (temp.height <= this.waterHeight) {
          if (left.isFlooded || right.isFlooded || top.isFlooded || bottom.isFlooded) {
            isFlooded = true;
            temp.isFlooded = true;
          }
        }
        Cell c = new Cell(temp.height, temp.x, temp.y, left, top, right, bottom, isFlooded);
        board = board.append(c);
      }
    }
    return board;
  }

  // produces the mountain island height values in the 2D Array
  ArrayList<ArrayList<Double>> initHeightsMountain() {
    ArrayList<ArrayList<Double>> h = new ArrayList<ArrayList<Double>>();
    for (int i = 0; i < ss.ISLAND_SIZE + 1; i += 1) {
      ArrayList<Double> row = new ArrayList<Double>();
      for (int j = 0; j < ss.ISLAND_SIZE + 1; j += 1) {
        int manhattanDist = Math.abs(i - (ss.ISLAND_SIZE / 2)) + Math.abs(j - (ss.ISLAND_SIZE / 2));
        double height = ss.MAX_HEIGHT - manhattanDist;
        if (height <= 0) {
          height = 0;
        }
        row.add(height);
      }
      h.add(row);
    }
    return h;
  }

  // produces the random island height values in the 2D Array
  ArrayList<ArrayList<Double>> initHeightsRandom() {
    ArrayList<ArrayList<Double>> h = new ArrayList<ArrayList<Double>>();
    double height;
    for (int i = 0; i < ss.ISLAND_SIZE + 1; i += 1) {
      ArrayList<Double> row = new ArrayList<Double>();
      for (int j = 0; j < ss.ISLAND_SIZE + 1; j += 1) {
        int manhattanDist = Math.abs(i - (ss.ISLAND_SIZE / 2)) + Math.abs(j - (ss.ISLAND_SIZE / 2));
        if (manhattanDist >= 32) {
          height = 0;
        }
        else {
          height = Math.random() * ss.MAX_HEIGHT + 1;
        }
        row.add(height);
      }
      h.add(row);
    }
    return h;
  }

  // produces the random terrain island height values in the 2D Array
  ArrayList<ArrayList<Double>> initHeightsTerrain() {
    ArrayList<ArrayList<Double>> h = this.terrainInit();

    int width = ss.ISLAND_SIZE / 2;

    int acc = 1;

    while (width >= 2) {
      for (int i = 0; i < Math.pow(2, acc); i++) {
        for (int j = 0; j < Math.pow(2, acc); j++) {
          this.terrainCoordinateHelp(i * width, i * width + width, j * width, j * width + width,
              width, h);
        }
      }
      width = width / 2;
      acc++;
    }
    return h;
  }

  // helper method which sets up the further steps of inHeightsTerrain
  // interp. sets the heights of the four midpoints
  public void terrainCoordinateHelp(int xl, int xr, int yt, int yb, int width,
      ArrayList<ArrayList<Double>> h) {
    double tl = h.get(xl).get(yt); // top left cell
    double tr = h.get(xr).get(yt); // top right cell
    double br = h.get(xr).get(yb); // bottom right cell
    double bl = h.get(xl).get(yb); // bottom left cell

    ArrayList<Double> temp = this.terrainHelp(tl, tr, bl, br, width); // IList
    // of top,
    // bottom,
    // left,
    // right,
    // middle
    // heights
    double height;

    height = temp.get(0); // top
    h.get((xl + xr) / 2).set(yt, height);
    height = temp.get(1); // bottom
    h.get((xl + xr) / 2).set(yb, height);
    height = temp.get(2); // left
    h.get(xl).set((yt + yb) / 2, height);
    height = temp.get(3); // right
    h.get(xr).set((yt + yb) / 2, height);
    height = temp.get(4); // middle
    h.get((xl + xr) / 2).set((yt + yb) / 2, height);
  }

  // another help which further sets up the values, unlike the prior this deals
  // directly with int values
  // interp. generates the heights for the midpoints and center
  public ArrayList<Double> terrainHelp(double tl, double tr, double bl, double br, int width) {
    ArrayList<Double> h = new ArrayList<Double>();
    double t = 0;
    double b = 0;
    double l = 0;
    double r = 0;
    double m = 0;

    t = Math.random() * (2 * width - 1) - width + Math.floor((tl + tr) / 2);
    if (t > 32) {
      t = 32;
    }
    else if (t < 0) {
      t = 0;
    }
    h.add(t);
    b = Math.random() * (2 * width - 1) - width + Math.floor((bl + br) / 2);
    if (b > 32) {
      b = 32;
    }
    else if (b < 0) {
      b = 0;
    }
    h.add(b);
    l = Math.random() * (2 * width - 1) - width + Math.floor((tl + br) / 2);
    if (l > 32) {
      l = 32;
    }
    else if (l < 0) {
      l = 0;
    }
    h.add(l);
    r = Math.random() * (2 * width - 1) - width + Math.floor((br + tr) / 2);
    if (r > 32) {
      r = 32;
    }
    else if (r < 0) {
      r = 0;
    }
    h.add(r);
    m = Math.random() * (2 * width - 1) - width + Math.floor((tl + bl + br + tr) / 4);
    if (m > 32) {
      m = 32;
    }
    else if (m < 0) {
      m = 0;
    }
    h.add(m);

    return h;
  }

  // adds the row and column heights for terrain
  ArrayList<ArrayList<Double>> terrainInit() {
    ArrayList<ArrayList<Double>> z = new ArrayList<ArrayList<Double>>();
    for (int i = 0; i < ss.ISLAND_SIZE + 1; i += 1) {
      ArrayList<Double> row = new ArrayList<Double>();
      for (int j = 0; j < ss.ISLAND_SIZE + 1; j += 1) {
        row.add(0.0);
      }
      z.add(row);
    }

    z.get(ss.ISLAND_SIZE / 2).set(ss.ISLAND_SIZE / 2, (double) ss.MAX_HEIGHT);

    z.get(0).set(ss.ISLAND_SIZE / 2, 1.0);
    z.get(ss.ISLAND_SIZE / 2).set(0, 1.0);
    z.get(ss.ISLAND_SIZE).set(ss.ISLAND_SIZE / 2, 1.0);
    z.get(ss.ISLAND_SIZE / 2).set(ss.ISLAND_SIZE, 1.0);

    return z;
  }

  // adds in the rows and columns correctly based on heights and sets isFlooded
  ArrayList<ArrayList<Cell>> initBoard() {
    boolean isFlooded;
    ArrayList<ArrayList<Cell>> b = new ArrayList<ArrayList<Cell>>();
    for (int i = 0; i < ss.ISLAND_SIZE + 1; i += 1) {
      ArrayList<Cell> row = new ArrayList<Cell>();
      for (int j = 0; j < ss.ISLAND_SIZE + 1; j += 1) {
        double h = this.heights.get(i).get(j);
        if (h == 0) {
          isFlooded = true;
        }
        else {
          isFlooded = false;
        }
        Cell c = new Cell(h, i, j, isFlooded);
        row.add(c);
      }
      b.add(row);
    }
    return b;
  }

  SceneSettings ss = new SceneSettings();

  // setting the background image for the scene
  public WorldImage background = new RectangleImage(ss.WIDTH, ss.HEIGHT, "solid", Color.black);

  // makes the scene using the Iterator
  public WorldScene makeScene() {
    if (isStarted) {
      WorldScene bg = this.getEmptyScene();
      Iterator<Cell> cellIter = this.board.iterator();

      while (cellIter.hasNext()) {
        Cell temp = cellIter.next();
        bg.placeImageXY(temp.getImage(this.waterHeight), temp.x * ss.CELL_SIZE,
            temp.y * ss.CELL_SIZE);
      }

      Iterator<Target> tarIter = this.targets.iterator();
      for (int j = 0; j < this.targets.length(); j++) {
        tarIter.next().draw(bg, this.waterHeight);
      }
      this.heli.draw(bg, this.waterHeight);
      this.player.draw(bg, this.waterHeight);
      this.player2.draw(bg, this.waterHeight);
      return bg;
    }
    else {
      WorldScene menu = this.getEmptyScene();
      menu.placeImageXY(new TextImage("Press m, r, or t to begin", 40, Color.RED), ss.WIDTH / 2,
          ss.HEIGHT / 2);
      return menu;
    }
  }

  // ontick method used for primarily raising the height of water
  public void onTick() {
    if (this.isStarted) {
      if (tickNum % 10 == 0) {
        this.waterHeight += 1;
        this.board = this.update();
      }
      this.tickNum += 1;
    }
  }

  // onkey method which calls the relevant methods and checks the
  // given inputs to make sure either a player moves or map switches
  // m t r are used for 1 player
  public void onKeyEvent(String key) {
    if (key.equals("r")) {
      this.isStarted = true;
      this.waterHeight = 0;
      this.heights = this.initHeightsRandom();
      this.cells = this.initBoard();
      this.board = this.toIList();
      this.targets = this.makeTargets();
      this.heli = this.makeHeli();
      this.player = this.playerSpawn();
      this.player2 = this.playerSpawn();
    }
    else if (key.equals("m")) {
      this.isStarted = true;
      this.waterHeight = 0;
      this.heights = this.initHeightsMountain();
      this.cells = this.initBoard();
      this.board = this.toIList();
      this.targets = this.makeTargets();
      this.heli = this.makeHeli();
      this.player = this.playerSpawn();
      this.player2 = this.playerSpawn();
    }
    else if (key.equals("t")) {
      this.isStarted = true;
      this.waterHeight = 0;
      this.heights = this.initHeightsTerrain();
      this.cells = this.initBoard();
      this.board = this.toIList();
      this.targets = this.makeTargets();
      this.heli = this.makeHeli();
      this.player = this.playerSpawn();
      this.player2 = this.playerSpawn();
    }
    else if (key.equals("right") || key.equals("left") || key.equals("up") || key.equals("down")) {
      this.player.move(key, this.cells.get(this.player.x).get(this.player.y));
      this.checkTargetCollected();
    }
    else if (key.equals("d") || key.equals("a") || key.equals("w") || key.equals("s")) {
      this.player2.move(key, this.cells.get(this.player2.x).get(this.player2.y));
      this.checkTargetCollected();
    }
  }

  // checks if either player collected a target and makes it
  // possible to collect the helicopter
  public void checkTargetCollected() {
    Iterator<Target> tarIter = this.targets.iterator();
    for (int i = 0; i < this.targets.length(); i++) {
      Target temp = tarIter.next();
      if (temp.samePosn(this.player) || temp.samePosn(this.player2)) {
        temp.isCollected = true;
        this.collected.add(temp);
        if (this.collected.size() == this.targets.length()) {
          this.heli.canBeCollected = true;
        }
      }
    }
  }

  // covers all world end cases, such as the players drowning, or targets being
  // under water
  public WorldEnd worldEnds() {
    if (this.isStarted) {
      Iterator<Target> tarIter = this.targets.iterator();
      for (int j = 0; j < this.targets.length(); j++) {
        Target temp = tarIter.next();
        if (this.cells.get(temp.x).get(temp.y).isFlooded && !temp.isCollected) {
          return new WorldEnd(true, this.lastScene("One of the targets was flooded!"));
        }
      }
      if (this.heli.canBeCollected && this.heli.samePosn(this.player)
          && this.heli.samePosn(this.player2)) {
        return new WorldEnd(true, this.lastScene("You both made it off the island!"));
      }
      else if (this.cells.get(this.heli.x).get(this.heli.y).isFlooded) {
        return new WorldEnd(true, this.lastScene("You lost the helicopter! Sucks :("));
      }
      else if (this.cells.get(this.player.x).get(this.player.y).isFlooded) {
        return new WorldEnd(true, this.lastScene("Player 1 drowned."));
      }
      else if (this.cells.get(this.player2.x).get(this.player2.y).isFlooded) {
        return new WorldEnd(true, this.lastScene("Player 2 drowned."));
      }
      return new WorldEnd(false, this.makeScene());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }

  // end scene representation
  public WorldScene lastScene(String s) {
    WorldScene bg = this.makeScene();
    bg.placeImageXY(new TextImage(s, 28, Color.red), ss.WIDTH / 2, ss.HEIGHT / 2);
    return bg;
  }
}

// predicates
interface IPred<T> {
  boolean apply(T t);
}

// same posn representation
class SamePosn implements IPred<Target> {
  int x;
  int y;

  // constructor
  SamePosn(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // apply set up to be used
  public boolean apply(Target t) {
    return t.x == this.x && t.y == this.y;
  }
}

// used for setting up the Ilist iterator
interface IList<T> extends Iterable<T> {
  IList<T> append(T t);

  int length();

  boolean isCons();

  ConsList<T> asCons();

  T getFirst();

  boolean ormap(IPred<T> pred);

  Iterator<T> iterator();
}

// for representing the cons, or rest case of the Ilist<T>
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  // constructor
  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  public IList<T> append(T t) {
    return new ConsList<T>(t, this);
  }

  public int length() {
    return 1 + this.rest.length();
  }

  public Iterator<T> iterator() {
    return new IListIterator<T>(this);
  }

  public boolean isCons() {
    return true;
  }

  public ConsList<T> asCons() {
    return this;
  }

  public T getFirst() {
    return this.first;
  }

  public boolean ormap(IPred<T> pred) {
    return pred.apply(this.first) || this.rest.ormap(pred);
  }
}

// for representing the empty case of the Ilist<T>
class MtList<T> implements IList<T> {

  public IList<T> append(T t) {
    return new ConsList<T>(t, this);
  }

  public int length() {
    return 0;
  }

  public Iterator<T> iterator() {
    return new IListIterator<T>(this);
  }

  public boolean isCons() {
    return false;
  }

  public ConsList<T> asCons() {
    throw new ClassCastException("Empty List Cannot Be Cast As Cons");
  }

  public T getFirst() {
    throw new RuntimeException("Empty List Does Not Have First Field");
  }

  public boolean ormap(IPred<T> pred) {
    return false;
  }
}

// used to let the Ilist be iterated upon
interface Iterable<T> {
  Iterator<T> iterator();
}

// Iterator interface which the is used on the Ilist
interface Iterator<T> {
  // is there at least one element to iterate over?
  boolean hasNext();

  // get the next item
  T next();
}

// As the parts are built above now the Ilist iterator can be made
class IListIterator<T> implements Iterator<T> {

  IList<T> items;

  IListIterator(IList<T> items) {
    this.items = items;
  }

  public boolean hasNext() {
    return this.items.isCons();
  }

  public T next() {
    ConsList<T> asList = this.items.asCons();
    T item = asList.first;
    this.items = asList.rest;
    return item;
  }
}

// Examples and testing
class ExamplesIsland {

  // to change example to a random please look at the comment on line 111
  ForbiddenIslandWorld w1;

  Cell c1;

  WorldScene bg;

  SceneSettings ss = new SceneSettings();

  // testing initMountain
  void initMountain() {
    this.w1 = new ForbiddenIslandWorld();
    this.w1.isStarted = true;
    this.w1.heights = this.w1.initHeightsMountain();
    this.w1.cells = this.w1.initBoard();
    this.w1.waterHeight = 0;
    this.w1.board = this.w1.toIList();
    this.w1.player = this.w1.playerSpawn();
    this.w1.targets = this.w1.makeTargets();
    this.w1.heli = this.w1.makeHeli();
    this.c1 = new Cell(8, 0, 0, false);
    this.bg = new ForbiddenIslandWorld().getEmptyScene();
  }

  void testGame(Tester t) {
    ForbiddenIslandWorld w = new ForbiddenIslandWorld();
    w.bigBang(ss.WIDTH, ss.HEIGHT, 0.3);
  }

  // testing initHeights
  void testInitHeights(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.heights.get(32).get(32), 32.0);
    for (int i = 0; i < 65; i++) {
      t.checkExpect(this.w1.heights.get(i).get(0), 0.0);
      t.checkExpect(this.w1.heights.get(0).get(i), 0.0);
      t.checkExpect(this.w1.heights.get(i).get(32), 32 - Math.abs((double) i - 32));
      t.checkExpect(this.w1.heights.get(32).get(i), 32 - Math.abs((double) i - 32));
    }
  }

  // testing initBoard
  void testInitBoard(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.cells.get(0).get(0).top, this.w1.cells.get(0).get(0));
    t.checkExpect(this.w1.cells.get(0).get(0).left, this.w1.cells.get(0).get(0));
    t.checkExpect(this.w1.cells.get(0).get(0).right, this.w1.cells.get(1).get(0));
    t.checkExpect(this.w1.cells.get(0).get(0).bottom, this.w1.cells.get(0).get(1));
    t.checkExpect(this.w1.cells.get(0).get(0).x, 0);
    t.checkExpect(this.w1.cells.get(0).get(0).y, 0);
    t.checkExpect(this.w1.cells.get(0).get(0).height, 0.0);
    t.checkExpect(this.w1.cells.get(32).get(32).height, 32.0);
    t.checkExpect(this.w1.cells.get(32).get(32).top, this.w1.cells.get(32).get(31));
    t.checkExpect(this.w1.cells.get(32).get(32).bottom, this.w1.cells.get(32).get(33));
    t.checkExpect(this.w1.cells.get(32).get(32).left, this.w1.cells.get(31).get(32));
    t.checkExpect(this.w1.cells.get(32).get(32).right, this.w1.cells.get(33).get(32));
    t.checkExpect(this.w1.cells.get(32).get(32).x, 32);
    t.checkExpect(this.w1.cells.get(32).get(32).y, 32);
  }

  // testingToIList via nested for loops to go over the produced cells properly
  void testToIList(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.board.iterator().hasNext(), true);
    Iterator<Cell> cellIter = this.w1.board.iterator();
    for (int i = 0; i < this.w1.cells.size(); i++) {
      for (int j = 0; j < this.w1.cells.size(); j++) {
        Cell temp = cellIter.next();
        t.checkExpect(temp.x, 64 - i);
        t.checkExpect(temp.y, 64 - j);
      }
    }
  }

  // testing Length
  void testLength(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.board.length(), 4225);
    // length is not 4096 due to the extra column and row
  }

  // testing Append
  void testAppend(Tester t) {
    this.initMountain();
    t.checkExpect(new MtList<Cell>().append(this.c1),
        new ConsList<Cell>(this.c1, new MtList<Cell>()));
  }

  // testing get image for Cell
  void testGetImage(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.cells.get(0).get(0).getImage(0),
        new RectangleImage(10, 10, OutlineMode.SOLID, Color.BLUE));
    t.checkExpect(this.w1.cells.get(32).get(32).getImage(0),
        new RectangleImage(10, 10, OutlineMode.SOLID, new Color(255, 255, 255)));
  }

  // testing get color for cell
  void testGetColor(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.cells.get(0).get(0).getColor(0), Color.BLUE);
    t.checkExpect(this.w1.cells.get(32).get(32).getColor(0), new Color(255, 255, 255));
  }

  // testing same posn for target
  void testSamePosn(Tester t) {
    this.initMountain();
    Target t1 = new Target(10, 10);
    Player p1 = new Player(20, 20);
    Player p2 = new Player(10, 10);
    t.checkExpect(t1.samePosn(p1), false);
    t.checkExpect(t1.samePosn(p2), true);
  }

  // testing player spawn
  void testPlayerSpawn(Tester t) {
    this.initMountain();
    t.checkInexact((double) this.w1.player.x, 32.0, 32.0);
    t.checkInexact((double) this.w1.player.y, 32.0, 32.0);
    Cell temp = this.w1.cells.get(this.w1.player.x).get(this.w1.player.y);
    t.checkExpect(temp.isFlooded, false);
  }

  // testing make heli
  void testMakeHeli(Tester t) {
    this.initMountain();
    t.checkInexact((double) this.w1.heli.x, 32.0, 32.0);
    t.checkInexact((double) this.w1.heli.y, 32.0, 32.0);
    Cell temp = this.w1.cells.get(this.w1.heli.x).get(this.w1.heli.y);
    t.checkExpect(temp.isFlooded, false);
    t.checkExpect(temp.height, 32.0);
  }

  // testing make targets
  void testMakeTargets(Tester t) {
    this.initMountain();
    t.checkInexact((double) this.w1.targets.iterator().next().x, 32.0, 32.0);
    t.checkInexact((double) this.w1.targets.iterator().next().y, 32.0, 32.0);
    Cell temp = this.w1.cells.get(this.w1.targets.iterator().next().x)
        .get(this.w1.targets.iterator().next().y);
    t.checkExpect(temp.isFlooded, false);
    t.checkExpect(this.w1.targets.length(), ss.numTargets - 1);
  }

  // testing update
  void testUpdate(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.update(), this.w1.board);
    this.w1.waterHeight = 1;
    this.w1.board = this.w1.update();
    t.checkExpect(this.w1.board.iterator().hasNext(), true);
    Iterator<Cell> cellIter = this.w1.board.iterator();
    for (int i = 0; i < this.w1.cells.size(); i++) {
      for (int j = 0; j < this.w1.cells.size(); j++) {
        Cell temp = cellIter.next();
        if (temp.height <= 1) {
          t.checkExpect(temp.isFlooded, true);
        }
        else {
          t.checkExpect(temp.isFlooded, false);
        }
      }
    }
  }

  // testing check target collected
  void testCheckTargetCollected(Tester t) {
    this.initMountain();
    t.checkExpect(this.w1.collected.size(), 0);
    this.w1.player.x = this.w1.targets.iterator().next().x;
    this.w1.player.y = this.w1.targets.iterator().next().y;
    this.w1.checkTargetCollected();
    t.checkExpect(this.w1.collected.size(), 1);
  }

  // testing apply in SamePosn class (IPred<Target>)
  void testSamePosnApply(Tester t) {
    this.initMountain();
    IPred<Target> sp = new SamePosn(-10, -10);
    Target t1 = new Target(-10, -10);
    Target t2 = new Target(10, -10);
    t.checkExpect(sp.apply(t1), true);
    t.checkExpect(sp.apply(t2), false);
    t.checkExpect(this.w1.targets.ormap(sp), false);
    sp = new SamePosn(this.w1.targets.iterator().next().x, this.w1.targets.iterator().next().y);
    t.checkExpect(this.w1.targets.ormap(sp), true);
  }

  // testing is cons in IList
  void testIsCons(Tester t) {
    this.initMountain();
    t.checkExpect(new MtList<Cell>().isCons(), false);
    t.checkExpect(this.w1.board.isCons(), true);
  }

  // testing as cons in IList
  void testAsCons(Tester t) {
    this.initMountain();
    t.checkException(new ClassCastException("Empty List Cannot Be Cast As Cons"),
        new MtList<Cell>(), "asCons");
    t.checkExpect(this.w1.board.asCons(), this.w1.board);
  }

  // testing get first
  void testGetFirst(Tester t) {
    this.initMountain();
    Target t1 = new Target(10, 10);
    Target t2 = new Target(100, 10);
    IList<Target> ts = new ConsList<Target>(t1, new MtList<Target>());
    t.checkExpect(ts.getFirst(), t1);
    ts = ts.append(t2);
    t.checkExpect(ts.getFirst(), t2);
    t.checkException(new RuntimeException("Empty List Does Not Have First Field"),
        new MtList<Cell>(), "getFirst");
  }

  // testing ormap
  void testOrmap(Tester t) {
    Target t1 = new Target(10, 10);
    IPred<Target> sp = new SamePosn(10, 10);
    IList<Target> ts = new ConsList<Target>(t1, new MtList<Target>());
    t.checkExpect(ts.ormap(sp), true);
    sp = new SamePosn(100, 10);
    t.checkExpect(ts.ormap(sp), false);
  }
}